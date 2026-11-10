
package org.grnet.endpoint.scanner.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.grnet.endpoint.scanner.runtime.context.RoleEndpointHolder;
import org.grnet.endpoint.scanner.runtime.entities.RoleEndpoint;
import org.grnet.endpoint.scanner.runtime.repositories.RoleEndpointRepository;
import org.grnet.endpoint.scanner.runtime.entitlements.EntitlementProvider;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Interceptor responsible for enforcing authorization on secured API endpoints.
 *
 * Authorization is based on:
 * - Roles configured for each secured endpoint.
 * - User entitlements.
 * - Resources referenced by request parameters.
 * - Relationships between resources.
 *
 * Supported authorization mechanisms include direct resource access,
 * collection-level access, role-only access and access through related resources.
 *
 * Resource relationships are resolved through registered
 * ResourceRelationshipResolver implementations.
 */
@Interceptor
@SecuredEndpoint
@Priority(10)
public class SecuredEndpointInterceptor {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    EntitlementProvider entitlementProvider;

    @Inject
    RoleEndpointRepository roleEndpointRepository;

    @Inject
    ApiResourceHolder apiResourceHolder;

    @Inject
    RoleEndpointHolder roleHolder;

    @ConfigProperty(name = "api.auth.entitlements.parent-group")
    String parentGroup;

    @Inject
    Instance<ResourceRelationshipResolver> relationshipResolvers;

    private static final Logger LOG = Logger.getLogger(SecuredEndpointInterceptor.class);

    /**
     * Intercepts secured endpoint invocations and verifies whether the current
     * user is authorized to execute the requested operation.
     *
     * The authorization process consists of:
     * 1. Bypassing authorization checks for super administrators.
     * 2. Retrieving the endpoint security configuration.
     * 3. Extracting the user's entitlements.
     * 4. Generating the secured endpoint identifier.
     * 5. Retrieving the roles configured for the endpoint.
     * 6. Extracting the resources referenced by the request.
     * 7. Evaluating the user's authorization against the required resources.
     *
     * @param context the invocation context of the intercepted method
     * @return the result of the intercepted method
     * @throws Exception if the intercepted method execution fails
     * @throws ForbiddenException if the user is not authorized
     */
    @AroundInvoke
    public Object checkAccess(InvocationContext context) throws Exception {

        // Super administrators bypass endpoint authorization checks.
        if (entitlementProvider.isSuperAdmin()) return context.proceed();

        Method method = context.getMethod();
        SecuredEndpoint secured = method.getAnnotation(SecuredEndpoint.class);

        if (secured == null) throw new ForbiddenException("No security configuration found!");

        List<String> entitlements = extractEntitlements();

        String securedEndpointId = generateSecuredEndpointId(
                getHttpMethod(method),
                buildFullPath(context, method)
        );

        List<RoleEndpoint> roleEndpoints = roleEndpointRepository.list("secured_endpoint_id", securedEndpointId);

        // Make the endpoint's configured roles available during the request.
        RoleEndpointHolder.set(roleEndpoints);

        RequestParams params = read(context, method);

        boolean hasAccess = checkEntitlement(entitlements, roleEndpoints, secured, params);

        if (!hasAccess) throw new ForbiddenException("Access denied.");

        return context.proceed();
    }

    // --------------------------------------------------
    // Entitlements
    // --------------------------------------------------

    /**
     * Extracts and normalizes the user's entitlements.
     *
     * The configured parent group prefix and role attribute are removed
     * from each entitlement.
     *
     * Examples:
     * group:pcc-api:members:role=member -> members
     * pcc:PREFIX:1:role=prefix_owner -> PREFIX:1
     *
     * @return the list of normalized user entitlements
     */
    private List<String> extractEntitlements() {

        return entitlementProvider.fetchEntitlements()
                .stream()
                .map(e -> {
                    String raw = e.getRaw();
                    String prefix = parentGroup + ":";

                    int idx = raw.indexOf(prefix);
                    String value = (idx != -1) ? raw.substring(idx + prefix.length()) : raw;

                    return value.replaceAll(":role=[^:]+", "");
                })
                .toList();
    }

    // --------------------------------------------------
    // Authorization
    // --------------------------------------------------

    /**
     * Evaluates whether the user's entitlements grant access to the
     * requested endpoint and its associated resources.
     *
     * Supported authorization mechanisms:
     * 1. Role-only authorization.
     * 2. Direct resource authorization.
     * 3. Collection-level authorization.
     * 4. Related resource authorization.
     *
     * At least one configured role must match the user's entitlements
     * and satisfy the resource authorization requirements.
     *
     * @param entitlements the user's normalized entitlements
     * @param roleEndpoints the roles configured for the secured endpoint
     * @param secured the endpoint security configuration
     * @param params the extracted request parameters
     * @return true if the user is authorized, false otherwise
     */
    private boolean checkEntitlement(List<String> entitlements, List<RoleEndpoint> roleEndpoints, SecuredEndpoint secured, RequestParams params
    ) {

        List<String> acceptedAccess = extractAcceptedAccess(secured, params);

        Set<String> normalizedEntitlements = entitlements.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        return roleEndpoints.stream().anyMatch(entry -> {

            String roleName = entry.getRoleName().toUpperCase();

            // Role-only authorization.
            if (normalizedEntitlements.contains(roleName)) return true;

            if (acceptedAccess.isEmpty()) {
                return normalizedEntitlements.stream()
                        .anyMatch(entitlement -> entitlement.equals(roleName)
                                || entitlement.startsWith(roleName + ":"));
            }

            // Evaluate each requested resource independently.
            return acceptedAccess.stream().anyMatch(access -> {

                String normalizedAccess = access.toUpperCase();
                String requiredEntitlement = roleName + ":" + normalizedAccess;

                // Direct resource authorization.
                if (normalizedEntitlements.contains(requiredEntitlement)) return true;

                // Collection-level authorization.
                if (!normalizedAccess.contains(":")) {
                    boolean hasCollectionAccess = normalizedEntitlements.stream()
                            .anyMatch(entitlement -> entitlement.startsWith(requiredEntitlement + ":"));

                    if (hasCollectionAccess) return true;
                }

                // Related resource authorization.
                return hasRelatedResourceAccess(roleName, normalizedEntitlements, acceptedAccess, normalizedAccess);
            });
        });
    }

    /**
     * Checks whether access to a requested resource can be granted through
     * an authorized related resource.
     *
     * Resource relationships are defined by ResourceRelationshipResolver
     * implementations.
     *
     * Example:
     * A user authorized for PREFIX:4 may access PROVIDER:3 if PREFIX:4
     * belongs to PROVIDER:3.
     *
     * However, relationship-based authorization must not grant access
     * to a different explicitly requested resource of the same type.
     *
     * For example:
     * - PREFIX:4 and PREFIX:5 belong to PROVIDER:3.
     * - The user is authorized for PREFIX:4 but not PREFIX:5.
     * - The user may access PROVIDER:3 through PREFIX:4.
     * - The user must not access PREFIX:5 through that relationship.
     *
     * @param roleName the role being evaluated
     * @param entitlements the user's normalized entitlements
     * @param allAcceptedAccess all resource requirements extracted from the request
     * @param targetAccess the specific resource currently being evaluated
     * @return true if access can be granted through a resource relationship
     */
    private boolean hasRelatedResourceAccess(String roleName, Set<String> entitlements, List<String> allAcceptedAccess, String targetAccess) {

        // Relationship checks require a specific target resource identifier.
        String[] targetParts = targetAccess.toUpperCase().split(":", 2);

        if (targetParts.length != 2) return false;

        String targetResourceName = targetParts[0];
        String targetResourceId = targetParts[1];

        for (ResourceRelationshipResolver resolver : relationshipResolvers) {

            if (!resolver.getTargetResourceName().equalsIgnoreCase(targetResourceName)) continue;

            String sourceResourceName = resolver.getSourceResourceName().toUpperCase();

            // Do not use a relationship to authorize a different explicitly requested source resource.
            boolean specificSourceRequested = allAcceptedAccess.stream()
                    .map(String::toUpperCase)
                    .anyMatch(access -> access.startsWith(sourceResourceName + ":"));

            if (specificSourceRequested) continue;

            String sourceEntitlementPrefix = roleName + ":" + sourceResourceName + ":";

            for (String entitlement : entitlements) {

                if (!entitlement.startsWith(sourceEntitlementPrefix)) continue;

                String sourceResourceId = entitlement.substring(sourceEntitlementPrefix.length());

                if (sourceResourceId.isBlank()) continue;

                // Verify that the authorized source resource belongs to the requested target resource.
                if (resolver.isRelatedTo(sourceResourceId, targetResourceId)) return true;
            }
        }

        return false;
    }

    // --------------------------------------------------
    // Accepted access extraction
    // --------------------------------------------------

    /**
     * Extracts the resource authorization requirements from the request.
     *
     * Resource references are defined through @ParamRef annotations
     * in the endpoint's @SecuredEndpoint configuration.
     *
     * Supported parameter types:
     * - PATH
     * - QUERY
     * - HEADER
     * - BODY
     *
     * @param secured the endpoint security configuration
     * @param params the extracted request parameters
     * @return the list of resource authorization requirements
     */
    private List<String> extractAcceptedAccess(SecuredEndpoint secured, RequestParams params) {

        Map<ParamType, Map<String, ParamRef>> refMaps = Arrays.stream(secured.params())
                .collect(Collectors.groupingBy(
                        ParamRef::type,
                        Collectors.toMap(ParamRef::param, ref -> ref)
                ));

        List<String> acceptedAccess = new ArrayList<>();

        // Extract resource references from PATH parameters.
        if (refMaps.containsKey(ParamType.PATH)) {
            processParams(params.path, refMaps.get(ParamType.PATH), acceptedAccess);
        }

        // Extract resource references from QUERY parameters.
        if (refMaps.containsKey(ParamType.QUERY)) {
            processParams(params.query, refMaps.get(ParamType.QUERY), acceptedAccess);
        }

        // Extract resource references from HEADER parameters.
        if (refMaps.containsKey(ParamType.HEADER)) {
            processParams(params.header, refMaps.get(ParamType.HEADER), acceptedAccess);
        }

        // Extract resource references from BODY parameters.
        if (refMaps.containsKey(ParamType.BODY)) {
            processParams(extractBody(params.body), refMaps.get(ParamType.BODY), acceptedAccess);
        }

        return acceptedAccess;
    }

    // --------------------------------------------------
    // Resource processing
    // --------------------------------------------------

    /**
     * Processes request parameters and resolves their associated resources.
     *
     * The primary resource can be resolved through:
     * 1. The statically configured referTo resource.
     * 2. referToField, which dynamically determines the resource type
     *    from another request parameter.
     *
     * Additional resources defined through alsoReferTo are added
     * as collection-level authorization requirements.
     *
     * @param values the extracted request parameter values
     * @param refMap the configured parameter-to-resource mappings
     * @param acceptedAccess the resulting authorization requirements
     */
    private void processParams(
            Map<String, Object> values,
            Map<String, ParamRef> refMap,
            List<String> acceptedAccess
    ) {

        for (var entry : values.entrySet()) {

            ParamRef ref = refMap.get(entry.getKey());

            if (ref == null) continue;

            // Resolve the primary resource associated with this parameter.
            Class<? extends ApiResource> resource;

            if (!ref.referToField().isBlank()) {

                // Dynamically resolve the resource type using another request parameter.
                Object dynamicResourceValue = values.get(ref.referToField());

                if (dynamicResourceValue == null) {
                    throw new IllegalStateException("Missing referToField value: " + ref.referToField());
                }

                resource = resolveResource(dynamicResourceValue.toString());

            } else {
                resource = ref.referTo();
            }

            // Add the primary resource requirement when a resource is configured.
            if (resource != null && resource != NoResource.class) {
                addAcceptedAccess(resource, entry.getValue().toString(), acceptedAccess);
            }

            // Additional resources represent collection-level authorization requirements.
            for (Class<? extends ApiResource> additionalResource : ref.alsoReferTo()) {
                addAcceptedAccess(additionalResource, null, acceptedAccess);
            }
        }
    }

    /**
     * Adds a resource authorization requirement to the accepted access list.
     *
     * When a resource identifier is provided, a specific resource requirement
     * is created. Otherwise, a collection-level requirement is created.
     *
     * Examples:
     * PREFIX + 4 -> PREFIX:4
     * PREFIX + null -> PREFIX
     *
     * @param resource the resource enum representing the resource type
     * @param resourceId the resource identifier, or null for collection access
     * @param acceptedAccess the list of authorization requirements
     */
    @SuppressWarnings("unchecked")
    private void addAcceptedAccess(
            Class<? extends ApiResource> resource,
            String resourceId,
            List<String> acceptedAccess
    ) {

        if (resource == null || !resource.isEnum()) {
            throw new IllegalStateException("Invalid resource mapping: " + resource);
        }

        Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) resource;
        String resourceType = enumClass.getEnumConstants()[0].name();

        // Add either a specific resource or a collection-level requirement.
        acceptedAccess.add(resourceId == null ? resourceType : resourceType + ":" + resourceId);
    }

    /**
     * Dynamically resolves an API resource class using its registered name.
     *
     * Resource metadata is retrieved from ApiResourceHolder, and the
     * corresponding resource class is loaded using the context class loader.
     *
     * @param apiResource the registered API resource name
     * @return the corresponding ApiResource enum class
     * @throws IllegalArgumentException if the resource is not registered
     * @throws RuntimeException if the resource class cannot be loaded
     */
    @SuppressWarnings("unchecked")
    private Class<? extends ApiResource> resolveResource(String apiResource) {

        var metadata = apiResourceHolder.getResources().stream()
                .filter(r -> r.getResourceName().equalsIgnoreCase(apiResource))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown api resource: " + apiResource));

        try {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(metadata.getClassName());
            return (Class<? extends ApiResource>) clazz;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot load resource class: " + metadata.getClassName(), e);
        }
    }

    // --------------------------------------------------
    // Request parameters
    // --------------------------------------------------

    /**
     * Holds the request parameters extracted from an intercepted endpoint.
     *
     * Parameters are separated according to their HTTP source:
     * - PATH
     * - QUERY
     * - HEADER
     * - BODY
     */
    public class RequestParams {

        public final Map<String, Object> path = new HashMap<>();
        public final Map<String, Object> query = new HashMap<>();
        public final Map<String, Object> header = new HashMap<>();
        public final List<Object> body = new ArrayList<>();
    }

    /**
     * Converts request body objects into a map of parameter values.
     *
     * Supports objects, maps and collections of objects.
     *
     * @param body the request body values
     * @return a map containing the extracted body fields
     */
    private Map<String, Object> extractBody(Object body) {

        Map<String, Object> result = new HashMap<>();
        extract(body, result);
        return result;
    }

    /**
     * Recursively extracts values from the supplied object.
     *
     * Maps are copied directly, iterables are processed item by item,
     * and regular objects are converted into maps using ObjectMapper.
     *
     * @param value the object to extract
     * @param result the map receiving the extracted values
     */
    @SuppressWarnings("unchecked")
    private void extract(Object value, Map<String, Object> result) {

        if (value == null) return;

        // Copy map entries directly.
        if (value instanceof Map<?, ?> map) {
            result.putAll((Map<String, Object>) map);
            return;
        }

        // Recursively process collections of objects.
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                extract(item, result);
            }
            return;
        }

        // Convert regular objects into maps using Jackson.
        result.putAll(objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {}));
    }

    /**
     * Extracts request parameters from the intercepted method invocation.
     *
     * Method parameters are identified through their JAX-RS annotations.
     * Framework and context parameters are excluded.
     * Parameters without a recognized HTTP parameter annotation are
     * treated as request body values.
     *
     * @param context the invocation context
     * @param method the intercepted endpoint method
     * @return the extracted request parameters
     */
    public RequestParams read(InvocationContext context, Method method) {

        RequestParams result = new RequestParams();
        Parameter[] params = method.getParameters();
        Object[] values = context.getParameters();

        for (int i = 0; i < params.length; i++) {

            Parameter p = params[i];
            Object value = values[i];

            // Extract PATH parameters.
            if (p.isAnnotationPresent(PathParam.class)) {
                result.path.put(p.getAnnotation(PathParam.class).value(), value);
                continue;
            }

            // Extract QUERY parameters.
            if (p.isAnnotationPresent(QueryParam.class)) {
                result.query.put(p.getAnnotation(QueryParam.class).value(), value);
                continue;
            }

            // Extract HEADER parameters.
            if (p.isAnnotationPresent(HeaderParam.class)) {
                result.header.put(p.getAnnotation(HeaderParam.class).value(), value);
                continue;
            }

            // Skip framework and context parameters.
            if (p.isAnnotationPresent(Context.class)
                    || value instanceof UriInfo
                    || value instanceof jakarta.ws.rs.core.SecurityContext
                    || value instanceof jakarta.ws.rs.core.HttpHeaders
                    || value instanceof jakarta.ws.rs.container.ContainerRequestContext) continue;

            // Parameters without HTTP annotations are treated as body values.
            result.body.add(value);
        }

        return result;
    }

    // --------------------------------------------------
    // Endpoint ID helpers
    // --------------------------------------------------

    /**
     * Generates a deterministic SHA-256 identifier for an endpoint.
     *
     * The identifier is generated using the HTTP method and normalized
     * endpoint path, allowing the endpoint to be matched against its
     * corresponding security configuration.
     *
     * @param httpMethod the HTTP method
     * @param fullPath the normalized endpoint path
     * @return the hexadecimal SHA-256 hash of the endpoint
     */
    private String generateSecuredEndpointId(String httpMethod, String fullPath) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((httpMethod + fullPath).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the HTTP method associated with the endpoint method.
     *
     * The HTTP method is identified through the @HttpMethod meta-annotation
     * of the corresponding JAX-RS annotation, such as @GET, @POST or @PUT.
     *
     * @param method the endpoint method
     * @return the HTTP method name
     * @throws IllegalStateException if no HTTP method annotation is found
     */
    private String getHttpMethod(Method method) {

        for (var annotation : method.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(HttpMethod.class)) {
                return annotation.annotationType().getAnnotation(HttpMethod.class).value();
            }
        }

        throw new IllegalStateException("No HTTP method annotation found on " + method.getName());
    }

    /**
     * Builds the complete endpoint path by combining the class-level
     * and method-level @Path annotations.
     *
     * The resulting path is normalized before being used to generate
     * the secured endpoint identifier.
     *
     * @param context the invocation context
     * @param method the endpoint method
     * @return the complete normalized endpoint path
     */
    private String buildFullPath(InvocationContext context, Method method) {

        var classPath = method.getDeclaringClass().getAnnotation(Path.class);
        var methodPath = method.getAnnotation(Path.class);

        String full = "";

        if (classPath != null) full += classPath.value();
        if (methodPath != null) full += methodPath.value();

        return normalizePath(full);
    }

    /**
     * Normalizes an endpoint path by ensuring that it starts with
     * a single slash and does not contain consecutive slashes.
     *
     * @param path the endpoint path
     * @return the normalized endpoint path
     */
    private String normalizePath(String path) {

        if (!path.startsWith("/")) path = "/" + path;

        return path.replaceAll("//+", "/");
    }
}
