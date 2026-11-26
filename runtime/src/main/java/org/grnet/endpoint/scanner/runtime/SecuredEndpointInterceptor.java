
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
        Method method = context.getMethod();
        String endpoint = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
        LOG.debugf("[AUTH] Starting authorization for %s", endpoint);

        // Super administrators bypass endpoint authorization checks.
        if (entitlementProvider.isSuperAdmin()) {
            LOG.debugf("[AUTH] Super administrator bypass for %s", endpoint);
            return context.proceed();
        }

        SecuredEndpoint secured = method.getAnnotation(SecuredEndpoint.class);
        if (secured == null) {
            LOG.warnf("[AUTH] No @SecuredEndpoint configuration found for %s", endpoint);
            throw new ForbiddenException("No security configuration found!");
        }

        List<String> entitlements = extractEntitlements();
        LOG.debugf("[AUTH] Extracted %d normalized entitlements", entitlements.size());

        String httpMethod = getHttpMethod(method);
        String fullPath = buildFullPath(context, method);
        String securedEndpointId = generateSecuredEndpointId(httpMethod, fullPath);
        LOG.debugf("[AUTH] Endpoint: %s %s; secured endpoint ID: %s", httpMethod, fullPath, securedEndpointId);

        List<RoleEndpoint> roleEndpoints = roleEndpointRepository.list("secured_endpoint_id", securedEndpointId);
        LOG.debugf("[AUTH] Found %d configured role mappings for endpoint", roleEndpoints.size());
        if (roleEndpoints.isEmpty()) {
            LOG.warnf("[AUTH] No role mappings found for endpoint ID %s", securedEndpointId);
        }

        // Make the endpoint's configured roles available during the request.
        RoleEndpointHolder.set(roleEndpoints);

        RequestParams params = read(context, method);
        boolean hasAccess = checkEntitlement(entitlements, roleEndpoints, secured, params);

        if (!hasAccess) {
            LOG.warnf("[AUTH] ACCESS DENIED for %s (%s %s)", endpoint, httpMethod, fullPath);
            throw new ForbiddenException("Access denied.");
        }

        LOG.debugf("[AUTH] ACCESS GRANTED for %s (%s %s)", endpoint, httpMethod, fullPath);
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
        List<String> normalized = entitlementProvider.fetchEntitlements()
                .stream()
                .map(e -> {
                    String raw = e.getRaw();
                    String prefix = parentGroup + ":";
                    int idx = raw.indexOf(prefix);
                    String value = (idx != -1) ? raw.substring(idx + prefix.length()) : raw;
                    return value.replaceAll(":role=[^:]+", "");
                })
                .toList();

        LOG.debugf("[AUTH] Normalized entitlements (count=%d)", normalized.size());
        if (LOG.isTraceEnabled()) {
            LOG.tracef("[AUTH] Normalized entitlement values: %s", normalized);
        }
        return normalized;
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
    private boolean checkEntitlement(List<String> entitlements, List<RoleEndpoint> roleEndpoints,
                                     SecuredEndpoint secured, RequestParams params) {
        List<String> acceptedAccess = extractAcceptedAccess(secured, params);
        Set<String> normalizedEntitlements = entitlements.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        LOG.debugf("[AUTH] Evaluating access: %d role mappings, %d resource requirements",
                roleEndpoints.size(), acceptedAccess.size());
        LOG.debugf("[AUTH] Required resource access: %s", acceptedAccess);

        for (RoleEndpoint entry : roleEndpoints) {
            String roleName = entry.getRoleName().toUpperCase();
            LOG.debugf("[AUTH] Checking configured role: %s", roleName);

            // Role-only authorization.
            if (normalizedEntitlements.contains(roleName)) {
                LOG.debugf("[AUTH] GRANTED by role-only entitlement: %s", roleName);
                return true;
            }

            if (acceptedAccess.isEmpty()) {
                boolean roleMatch = normalizedEntitlements.stream()
                        .anyMatch(entitlement -> entitlement.equals(roleName)
                                || entitlement.startsWith(roleName + ":"));
                LOG.debugf("[AUTH] Role-only entitlement-prefix match for %s: %s", roleName, roleMatch);
                if (roleMatch) return true;
                continue;
            }

            // Preserve the existing behavior: any matching requested access grants this role.
            for (String access : acceptedAccess) {
                String normalizedAccess = access.toUpperCase();
                String requiredEntitlement = roleName + ":" + normalizedAccess;
                LOG.debugf("[AUTH] Checking required entitlement: %s", requiredEntitlement);

                // Direct resource authorization.
                if (normalizedEntitlements.contains(requiredEntitlement)) {
                    LOG.debugf("[AUTH] GRANTED by direct resource entitlement: %s", requiredEntitlement);
                    return true;
                }

                // Collection-level authorization.
                if (!normalizedAccess.contains(":")) {
                    boolean hasCollectionAccess = normalizedEntitlements.stream()
                            .anyMatch(entitlement -> entitlement.startsWith(requiredEntitlement + ":"));
                    LOG.debugf("[AUTH] Collection-level match for %s: %s", requiredEntitlement, hasCollectionAccess);
                    if (hasCollectionAccess) return true;
                }

                // Related resource authorization.
                if (hasRelatedResourceAccess(roleName, normalizedEntitlements, acceptedAccess, normalizedAccess)) {
                    LOG.debugf("[AUTH] GRANTED through related resource for %s", normalizedAccess);
                    return true;
                }
            }
        }

        LOG.debug("[AUTH] No configured role matched the user's entitlements and requested resources");
        return false;
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
    private boolean hasRelatedResourceAccess(String roleName, Set<String> entitlements,
                                             List<String> allAcceptedAccess, String targetAccess) {
        String[] targetParts = targetAccess.toUpperCase().split(":", 2);
        if (targetParts.length != 2) {
            LOG.debugf("[AUTH] Related-resource check skipped; target is not a specific resource: %s", targetAccess);
            return false;
        }

        String targetResourceName = targetParts[0];
        String targetResourceId = targetParts[1];
        LOG.debugf("[AUTH] Looking for related-resource access to %s:%s", targetResourceName, targetResourceId);

        for (ResourceRelationshipResolver resolver : relationshipResolvers) {
            if (!resolver.getTargetResourceName().equalsIgnoreCase(targetResourceName)) continue;

            String sourceResourceName = resolver.getSourceResourceName().toUpperCase();
            LOG.debugf("[AUTH] Applicable relationship resolver: %s -> %s", sourceResourceName, targetResourceName);

            // Do not use a relationship to authorize a different explicitly requested source resource.
            boolean specificSourceRequested = allAcceptedAccess.stream()
                    .map(String::toUpperCase)
                    .anyMatch(access -> access.startsWith(sourceResourceName + ":"));
            if (specificSourceRequested) {
                LOG.debugf("[AUTH] Related-resource check skipped; source %s is explicitly requested", sourceResourceName);
                continue;
            }

            String sourceEntitlementPrefix = roleName + ":" + sourceResourceName + ":";
            for (String entitlement : entitlements) {
                if (!entitlement.startsWith(sourceEntitlementPrefix)) continue;

                String sourceResourceId = entitlement.substring(sourceEntitlementPrefix.length());
                if (sourceResourceId.isBlank()) continue;

                LOG.debugf("[AUTH] Checking relationship %s:%s -> %s:%s",
                        sourceResourceName, sourceResourceId, targetResourceName, targetResourceId);
                if (resolver.isRelatedTo(sourceResourceId, targetResourceId)) {
                    LOG.debugf("[AUTH] Related-resource match found: %s:%s -> %s:%s",
                            sourceResourceName, sourceResourceId, targetResourceName, targetResourceId);
                    return true;
                }
            }
        }

        LOG.debugf("[AUTH] No related-resource entitlement grants access to %s", targetAccess);
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
                .collect(Collectors.groupingBy(ParamRef::type,
                        Collectors.toMap(ParamRef::param, ref -> ref)));

        LOG.debugf("[AUTH] Configured ParamRef groups: %s", refMaps.keySet());
        List<String> acceptedAccess = new ArrayList<>();

        if (refMaps.containsKey(ParamType.PATH)) processParams(params.path, refMaps.get(ParamType.PATH), acceptedAccess);
        if (refMaps.containsKey(ParamType.QUERY)) processParams(params.query, refMaps.get(ParamType.QUERY), acceptedAccess);
        if (refMaps.containsKey(ParamType.HEADER)) processParams(params.header, refMaps.get(ParamType.HEADER), acceptedAccess);
        if (refMaps.containsKey(ParamType.BODY)) processParams(extractBody(params.body), refMaps.get(ParamType.BODY), acceptedAccess);

        LOG.debugf("[AUTH] Extracted accepted resource access: %s", acceptedAccess);
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
    private void processParams(Map<String, Object> values, Map<String, ParamRef> refMap,
                               List<String> acceptedAccess) {
        LOG.debugf("[AUTH] Processing parameter names: %s", values.keySet());
        for (var entry : values.entrySet()) {
            ParamRef ref = refMap.get(entry.getKey());
            if (ref == null) {
                LOG.tracef("[AUTH] No ParamRef configured for parameter '%s'", entry.getKey());
                continue;
            }

            LOG.debugf("[AUTH] Applying ParamRef for '%s' (type=%s)", entry.getKey(), ref.type());
            Class<? extends ApiResource> resource;
            if (!ref.referToField().isBlank()) {
                Object dynamicResourceValue = values.get(ref.referToField());
                LOG.debugf("[AUTH] Dynamic resource field '%s' resolved to '%s'",
                        ref.referToField(), dynamicResourceValue);
                if (dynamicResourceValue == null) {
                    LOG.errorf("[AUTH] Missing referToField value: %s", ref.referToField());
                    throw new IllegalStateException("Missing referToField value: " + ref.referToField());
                }
                resource = resolveResource(dynamicResourceValue.toString());
            } else {
                resource = ref.referTo();
            }

            if (resource != null && resource != NoResource.class) {
                addAcceptedAccess(resource, entry.getValue().toString(), acceptedAccess);
            } else {
                LOG.debugf("[AUTH] No primary resource added for parameter '%s'", entry.getKey());
            }

            for (Class<? extends ApiResource> additionalResource : ref.alsoReferTo()) {
                LOG.debugf("[AUTH] Adding alsoReferTo collection resource: %s", additionalResource.getSimpleName());
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
    private void addAcceptedAccess(Class<? extends ApiResource> resource, String resourceId,
                                   List<String> acceptedAccess) {
        if (resource == null || !resource.isEnum()) {
            LOG.errorf("[AUTH] Invalid resource mapping: %s", resource);
            throw new IllegalStateException("Invalid resource mapping: " + resource);
        }

        Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) resource;
        String resourceType = enumClass.getEnumConstants()[0].name();
        String access = resourceId == null ? resourceType : resourceType + ":" + resourceId;
        acceptedAccess.add(access);
        LOG.debugf("[AUTH] Added accepted access requirement: %s", access);
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
        LOG.debugf("[AUTH] Resolving API resource name: %s", apiResource);
        var metadata = apiResourceHolder.getResources().stream()
                .filter(r -> r.getResourceName().equalsIgnoreCase(apiResource))
                .findFirst()
                .orElseThrow(() -> {
                    LOG.errorf("[AUTH] Unknown API resource: %s", apiResource);
                    return new IllegalArgumentException("Unknown api resource: " + apiResource);
                });

        try {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(metadata.getClassName());
            LOG.debugf("[AUTH] Resolved resource '%s' to class %s", apiResource, clazz.getName());
            return (Class<? extends ApiResource>) clazz;
        } catch (ClassNotFoundException e) {
            LOG.errorf(e, "[AUTH] Cannot load resource class: %s", metadata.getClassName());
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
        LOG.debugf("[AUTH] Extracted body field names: %s", result.keySet());
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
        if (value instanceof Map<?, ?> map) {
            result.putAll((Map<String, Object>) map);
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) extract(item, result);
            return;
        }
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

            if (p.isAnnotationPresent(PathParam.class)) {
                String name = p.getAnnotation(PathParam.class).value();
                result.path.put(name, value);
                LOG.debugf("[AUTH] Found PATH parameter '%s'", name);
                continue;
            }
            if (p.isAnnotationPresent(QueryParam.class)) {
                String name = p.getAnnotation(QueryParam.class).value();
                result.query.put(name, value);
                LOG.debugf("[AUTH] Found QUERY parameter '%s'", name);
                continue;
            }
            if (p.isAnnotationPresent(HeaderParam.class)) {
                String name = p.getAnnotation(HeaderParam.class).value();
                result.header.put(name, value);
                LOG.debugf("[AUTH] Found HEADER parameter '%s'", name);
                continue;
            }
            if (p.isAnnotationPresent(Context.class)
                    || value instanceof UriInfo
                    || value instanceof jakarta.ws.rs.core.SecurityContext
                    || value instanceof jakarta.ws.rs.core.HttpHeaders
                    || value instanceof jakarta.ws.rs.container.ContainerRequestContext) {
                continue;
            }

            result.body.add(value);
            LOG.debugf("[AUTH] Found BODY parameter '%s'", p.getName());
        }

        LOG.debugf("[AUTH] Request parameter extraction complete: PATH=%s, QUERY=%s, HEADER=%s, BODY count=%d",
                result.path.keySet(), result.query.keySet(), result.header.keySet(), result.body.size());
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
            String id = HexFormat.of().formatHex(hash);
            LOG.debugf("[AUTH] Generated secured endpoint ID for %s %s: %s", httpMethod, fullPath, id);
            return id;
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
                String httpMethod = annotation.annotationType().getAnnotation(HttpMethod.class).value();
                LOG.tracef("[AUTH] Detected HTTP method: %s", httpMethod);
                return httpMethod;
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

        String normalized = normalizePath(full);
        LOG.debugf("[AUTH] Built endpoint path: %s", normalized);
        return normalized;
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