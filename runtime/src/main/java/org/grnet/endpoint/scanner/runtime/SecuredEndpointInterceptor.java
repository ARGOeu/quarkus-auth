
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

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

    private static final Logger LOG =
            Logger.getLogger(SecuredEndpointInterceptor.class);

    @AroundInvoke
    public Object checkAccess(InvocationContext context) throws Exception {

        if (entitlementProvider.isSuperAdmin()) {
            return context.proceed();
        }

        Method method = context.getMethod();

        SecuredEndpoint secured =
                method.getAnnotation(SecuredEndpoint.class);

        if (secured == null) {
            throw new ForbiddenException(
                    "No security configuration found!"
            );
        }

        List<String> entitlements = extractEntitlements();

        String securedEndpointId = generateSecuredEndpointId(
                getHttpMethod(method),
                buildFullPath(context, method)
        );

        List<RoleEndpoint> roleEndpoints =
                roleEndpointRepository.list(
                        "secured_endpoint_id",
                        securedEndpointId
                );

        RoleEndpointHolder.set(roleEndpoints);

        RequestParams params = read(context, method);

        boolean hasAccess = checkEntitlement(
                entitlements,
                roleEndpoints,
                secured,
                params
        );

        if (!hasAccess) {
            throw new ForbiddenException("Access denied.");
        }

        return context.proceed();
    }

    // --------------------------------------------------
    // Entitlements
    // --------------------------------------------------

    private List<String> extractEntitlements() {

        return entitlementProvider.fetchEntitlements()
                .stream()
                .map(e -> {

                    String raw = e.getRaw();
                    String prefix = parentGroup + ":";

                    int idx = raw.indexOf(prefix);

                    String value = (idx != -1)
                            ? raw.substring(idx + prefix.length())
                            : raw;

                    return value.replaceAll(":role=[^:]+", "");
                })
                .toList();
    }

    // --------------------------------------------------
    // Authorization
    // --------------------------------------------------
    private boolean checkEntitlement(
            List<String> entitlements,
            List<RoleEndpoint> roleEndpoints,
            SecuredEndpoint secured,
            RequestParams params
    ) {

        List<String> acceptedAccess =
                extractAcceptedAccess(secured, params);

        Set<String> normalizedEntitlements =
                entitlements.stream()
                        .map(String::toUpperCase)
                        .collect(Collectors.toSet());

        return roleEndpoints.stream()
                .anyMatch(entry -> {

                    String roleName = entry.getRoleName().toUpperCase();

                    // Role-only entitlement.
                    if (normalizedEntitlements.contains(roleName)) {
                        return true;
                    }

                    if (acceptedAccess.isEmpty()) {
                        return normalizedEntitlements.stream()
                                .anyMatch(entitlement ->
                                        entitlement.equals(roleName)
                                                || entitlement.startsWith(
                                                roleName + ":"
                                        )
                                );
                    }

                    // Every requested resource must be authorized.
                    return acceptedAccess.stream()
                            .allMatch(access -> {

                                String normalizedAccess =
                                        access.toUpperCase();

                                String requiredEntitlement =
                                        roleName + ":" + normalizedAccess;

                                // Direct resource authorization.
                                if (normalizedEntitlements.contains(
                                        requiredEntitlement)) {
                                    return true;
                                }

                                // Collection-level authorization.
                                if (!normalizedAccess.contains(":")) {
                                    boolean hasCollectionAccess =
                                            normalizedEntitlements.stream()
                                                    .anyMatch(entitlement ->
                                                            entitlement.startsWith(
                                                                    requiredEntitlement + ":"
                                                            )
                                                    );

                                    if (hasCollectionAccess) {
                                        return true;
                                    }
                                }

                                // Related resource authorization.
                                return hasRelatedResourceAccess(
                                        roleName,
                                        normalizedEntitlements,
                                        List.of(normalizedAccess)
                                );
                            });
                });
    }
    private boolean hasRelatedResourceAccess(
            String roleName,
            Set<String> entitlements,
            List<String> acceptedAccess
    ) {

        for (String access : acceptedAccess) {

            String[] targetParts = access.toUpperCase().split(":", 2);

            // Relationship checks require a specific target resource ID.
            if (targetParts.length != 2) {
                continue;
            }

            String targetResourceName = targetParts[0];
            String targetResourceId = targetParts[1];

            for (ResourceRelationshipResolver resolver : relationshipResolvers) {

                if (!resolver.getTargetResourceName()
                        .equalsIgnoreCase(targetResourceName)) {
                    continue;
                }

                String sourceEntitlementPrefix =
                        roleName + ":"
                                + resolver.getSourceResourceName().toUpperCase()
                                + ":";

                for (String entitlement : entitlements) {

                    if (!entitlement.startsWith(sourceEntitlementPrefix)) {
                        continue;
                    }

                    String sourceResourceId =
                            entitlement.substring(sourceEntitlementPrefix.length());

                    if (sourceResourceId.isBlank()) {
                        continue;
                    }

                    if (resolver.isRelatedTo(
                            sourceResourceId,
                            targetResourceId
                    )) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
    // --------------------------------------------------
    // Accepted access extraction
    // --------------------------------------------------

    private List<String> extractAcceptedAccess(
            SecuredEndpoint secured,
            RequestParams params
    ) {

        Map<ParamType, Map<String, ParamRef>> refMaps =
                Arrays.stream(secured.params())
                        .collect(Collectors.groupingBy(
                                ParamRef::type,
                                Collectors.toMap(
                                        ParamRef::param,
                                        ref -> ref
                                )
                        ));

        List<String> acceptedAccess = new ArrayList<>();

        // PATH params
        if (refMaps.containsKey(ParamType.PATH)) {
            processParams(
                    params.path,
                    refMaps.get(ParamType.PATH),
                    acceptedAccess
            );
        }

        // QUERY params
        if (refMaps.containsKey(ParamType.QUERY)) {
            processParams(
                    params.query,
                    refMaps.get(ParamType.QUERY),
                    acceptedAccess
            );
        }

        // HEADER params
        if (refMaps.containsKey(ParamType.HEADER)) {
            processParams(
                    params.header,
                    refMaps.get(ParamType.HEADER),
                    acceptedAccess
            );
        }

        // BODY params
        if (refMaps.containsKey(ParamType.BODY)) {
            processParams(
                    extractBody(params.body),
                    refMaps.get(ParamType.BODY),
                    acceptedAccess
            );
        }

        return acceptedAccess;
    }

    // --------------------------------------------------
    // Resource processing
    // --------------------------------------------------

    private void processParams(
            Map<String, Object> values,
            Map<String, ParamRef> refMap,
            List<String> acceptedAccess
    ) {

        for (var entry : values.entrySet()) {

            ParamRef ref = refMap.get(entry.getKey());

            if (ref == null) {
                continue;
            }

            // Primary resource: RESOURCE:id
            Class<? extends ApiResource> resource;

            if (!ref.referToField().isBlank()) {

                Object dynamicResourceValue =
                        values.get(ref.referToField());

                if (dynamicResourceValue == null) {
                    throw new IllegalStateException(
                            "Missing referToField value: "
                                    + ref.referToField()
                    );
                }

                resource = resolveResource(
                        dynamicResourceValue.toString()
                );

            } else {
                resource = ref.referTo();
            }

            if (resource != null && resource != NoResource.class) {

                addAcceptedAccess(
                        resource,
                        entry.getValue().toString(),
                        acceptedAccess
                );
            }

            // Additional resources: RESOURCE
            // These represent collection-level access.
            for (Class<? extends ApiResource> additionalResource
                    : ref.alsoReferTo()) {

                addAcceptedAccess(
                        additionalResource,
                        null,
                        acceptedAccess
                );
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addAcceptedAccess(
            Class<? extends ApiResource> resource,
            String resourceId,
            List<String> acceptedAccess
    ) {

        if (resource == null || !resource.isEnum()) {
            throw new IllegalStateException(
                    "Invalid resource mapping: " + resource
            );
        }

        Class<? extends Enum<?>> enumClass =
                (Class<? extends Enum<?>>) resource;

        String resourceType =
                enumClass.getEnumConstants()[0].name();

        acceptedAccess.add(
                resourceId == null
                        ? resourceType
                        : resourceType + ":" + resourceId
        );
    }

    @SuppressWarnings("unchecked")
    private Class<? extends ApiResource> resolveResource(
            String apiResource
    ) {

        var metadata = apiResourceHolder.getResources()
                .stream()
                .filter(r ->
                        r.getResourceName()
                                .equalsIgnoreCase(apiResource)
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown api resource: " + apiResource
                        )
                );

        try {

            Class<?> clazz =
                    Thread.currentThread()
                            .getContextClassLoader()
                            .loadClass(metadata.getClassName());

            return (Class<? extends ApiResource>) clazz;

        } catch (ClassNotFoundException e) {

            throw new RuntimeException(
                    "Cannot load resource class: "
                            + metadata.getClassName(),
                    e
            );
        }
    }

    // --------------------------------------------------
    // Request parameters
    // --------------------------------------------------

    public class RequestParams {

        public final Map<String, Object> path = new HashMap<>();
        public final Map<String, Object> query = new HashMap<>();
        public final Map<String, Object> header = new HashMap<>();
        public final List<Object> body = new ArrayList<>();
    }

    private Map<String, Object> extractBody(Object body) {

        Map<String, Object> result = new HashMap<>();

        extract(body, result);

        return result;
    }

    @SuppressWarnings("unchecked")
    private void extract(Object value, Map<String, Object> result) {

        if (value == null) {
            return;
        }

        if (value instanceof Map<?, ?> map) {
            result.putAll((Map<String, Object>) map);
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                extract(item, result);
            }
            return;
        }

        result.putAll(
                objectMapper.convertValue(
                        value,
                        new TypeReference<Map<String, Object>>() {}
                )
        );
    }

    public RequestParams read(
            InvocationContext context,
            Method method
    ) {

        RequestParams result = new RequestParams();

        Parameter[] params = method.getParameters();
        Object[] values = context.getParameters();

        for (int i = 0; i < params.length; i++) {

            Parameter p = params[i];
            Object value = values[i];

            if (p.isAnnotationPresent(PathParam.class)) {
                result.path.put(
                        p.getAnnotation(PathParam.class).value(),
                        value
                );
                continue;
            }

            if (p.isAnnotationPresent(QueryParam.class)) {
                result.query.put(
                        p.getAnnotation(QueryParam.class).value(),
                        value
                );
                continue;
            }

            if (p.isAnnotationPresent(HeaderParam.class)) {
                result.header.put(
                        p.getAnnotation(HeaderParam.class).value(),
                        value
                );
                continue;
            }

            // Skip framework/context parameters
            if (p.isAnnotationPresent(Context.class)
                    || value instanceof UriInfo
                    || value instanceof jakarta.ws.rs.core.SecurityContext
                    || value instanceof jakarta.ws.rs.core.HttpHeaders
                    || value instanceof jakarta.ws.rs.container.ContainerRequestContext) {
                continue;
            }

            // BODY fallback
            result.body.add(value);
        }

        return result;
    }

    // --------------------------------------------------
    // Endpoint ID helpers
    // --------------------------------------------------

    private String generateSecuredEndpointId(
            String httpMethod,
            String fullPath
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    (httpMethod + fullPath)
                            .getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String getHttpMethod(Method method) {

        for (var annotation : method.getAnnotations()) {

            if (annotation.annotationType()
                    .isAnnotationPresent(HttpMethod.class)) {

                return annotation.annotationType()
                        .getAnnotation(HttpMethod.class)
                        .value();
            }
        }

        throw new IllegalStateException(
                "No HTTP method annotation found on "
                        + method.getName()
        );
    }

    private String buildFullPath(
            InvocationContext context,
            Method method
    ) {

        var classPath =
                method.getDeclaringClass().getAnnotation(Path.class);

        var methodPath = method.getAnnotation(Path.class);

        String full = "";

        if (classPath != null) {
            full += classPath.value();
        }

        if (methodPath != null) {
            full += methodPath.value();
        }

        return normalizePath(full);
    }

    private String normalizePath(String path) {

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return path.replaceAll("//+", "/");
    }
}
