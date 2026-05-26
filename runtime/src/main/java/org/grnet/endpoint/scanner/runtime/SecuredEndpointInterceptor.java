package org.grnet.endpoint.scanner.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.grnet.endpoint.scanner.runtime.context.RoleEndpointHolder;
import org.grnet.endpoint.scanner.runtime.entities.RoleEndpoint;
import org.grnet.endpoint.scanner.runtime.repositories.RoleEndpointRepository;
import org.grnet.endpoint.scanner.runtime.entitlements.Entitlement;
import org.grnet.endpoint.scanner.runtime.entitlements.EntitlementProvider;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
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

    private static final Logger LOG = Logger.getLogger(SecuredEndpointInterceptor.class);
    private List<RoleEndpoint> ROLE_ENDPOINTS = new ArrayList<>();

    @AroundInvoke
    public Object checkAccess(InvocationContext context) throws Exception {

        if (entitlementProvider.isSuperAdmin()) {
            return context.proceed();
        }

        var method = context.getMethod();
        var secured = method.getAnnotation(SecuredEndpoint.class);

        if (secured == null) {
            throw new ForbiddenException("No security configuration found!");
        }

        var entitlements = extractEntitlements();

        var securedEndpointId = generateSecuredEndpointId(
                getHttpMethod(method),
                buildFullPath(context, method)
        );

        ROLE_ENDPOINTS = roleEndpointRepository.list("secured_endpoint_id", securedEndpointId);

        // roleEndpointContext.setRoleEndpoints(ROLE_ENDPOINTS);
        //context.getContextData().put("ROLE_ENDPOINTS", ROLE_ENDPOINTS);
        RoleEndpointHolder.set(ROLE_ENDPOINTS);
        RequestParams params = read(context, method);

        boolean hasAccess = checkEntitlement(
                entitlements,
                secured,
                params
        );
        //
        if (!hasAccess) {
            throw new ForbiddenException("Access denied.");
        }

        return context.proceed();
    }

    private List<String> extractEntitlements() {
        return entitlementProvider.fetchEntitlements().stream()
                .map(e -> {
                    String raw = e.getRaw();
                    String prefix = "status-pages:";
                    int idx = raw.indexOf(prefix);

                    String value = (idx != -1)
                            ? raw.substring(idx + prefix.length())
                            : raw;

                    return value.replaceAll(":role=[^:]+", "");
                })
                .toList();
    }

    private boolean checkEntitlement(
            List<String> entitlements,
            SecuredEndpoint secured,
            RequestParams params
    ) {

        List<String> acceptedAccess = extractAcceptedAccess(secured, params);

        Set<String> normalizedEntitlements = entitlements.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        return ROLE_ENDPOINTS.stream()
                .anyMatch(entry -> {

                    String roleName =
                            entry.getRoleName().toUpperCase();
                    // No params/access rules → role-only check
                    if (acceptedAccess.isEmpty()) {
                        return normalizedEntitlements.stream()
                                .anyMatch(entitlement ->
                                        entitlement.contains(roleName)
                                );
                    }
                    // Param/resource-based check
                    return acceptedAccess.stream()
                            .map(String::toUpperCase)
                            .anyMatch(access ->
                                    normalizedEntitlements.contains(
                                            (roleName + ":" + access).toUpperCase()
                                    )
                            );
                });
    }

    private List<String> extractAcceptedAccess(
            SecuredEndpoint secured,
            RequestParams params
    ) {

        Map<ParamType, Map<String, Object>> sources = Map.of(
                ParamType.PATH, params.path,
                ParamType.BODY, extractBody(params.body),
                ParamType.QUERY, params.query,
                ParamType.HEADER, params.header
        );

        Map<ParamType, Map<String, ParamRef>> refMaps =
                Arrays.stream(secured.params())
                        .collect(Collectors.groupingBy(
                                ParamRef::type,
                                Collectors.toMap(ParamRef::param, Function.identity())
                        ));

        List<String> acceptedAccess = new ArrayList<>();

        for (var type : sources.keySet()) {
            processParams(
                    sources.get(type),
                    refMaps.getOrDefault(type, Map.of()),
                    acceptedAccess
            );
        }

        return acceptedAccess;
    }

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

            Class<? extends ApiResource> resource;

            // Dynamic resource resolution
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

                // Static resource resolution
                resource = ref.referTo();
            }

            if (resource == null || resource == NoResource.class) {
                throw new IllegalStateException(
                        "No resource mapping configured for param: "
                                + ref.param()
                );
            }

            if (!resource.isEnum()) {
                throw new IllegalStateException(
                        resource + " is not an enum"
                );
            }

            Class<? extends Enum<?>> enumClass =
                    (Class<? extends Enum<?>>) resource;

            String resourceType =
                    enumClass.getEnumConstants()[0].name();

            String resourceValue =
                    resourceType + ":" + entry.getValue();

            acceptedAccess.add(resourceValue);
        }
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

    public class RequestParams {

        public final Map<String, Object> path = new HashMap<>();
        public final Map<String, Object> query = new HashMap<>();
        public final Map<String, Object> header = new HashMap<>();
        public final List<Object> body = new ArrayList<>();

    }

    private Map<String, Object> extractBody(Object body) {

        if (body == null) {
            return Map.of();
        }

        if (body instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }

        if (body instanceof Iterable<?> iterable) {

            Map<String, Object> result = new HashMap<>();

            for (Object item : iterable) {

                if (item == null) {
                    continue;
                }

                // skip framework objects
                if (item instanceof UriInfo) {
                    continue;
                }

                Map<String, Object> extracted =
                        objectMapper.convertValue(
                                item,
                                new TypeReference<Map<String, Object>>() {
                                }
                        );

                result.putAll(extracted);
            }

            return result;
        }

        return objectMapper.convertValue(
                body,
                new TypeReference<Map<String, Object>>() {
                }
        );
    }

    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == String.class
                || Number.class.isAssignableFrom(clazz)
                || clazz == Boolean.class
                || clazz == Character.class;
    }


    public RequestParams read(InvocationContext context, Method method) {

        RequestParams result = new RequestParams();

        Parameter[] params = method.getParameters();
        Object[] values = context.getParameters();

        for (int i = 0; i < params.length; i++) {

            Parameter p = params[i];
            Object value = values[i];

            if (p.isAnnotationPresent(PathParam.class)) {
                result.path.put(p.getAnnotation(PathParam.class).value(), value);
                continue;
            }

            if (p.isAnnotationPresent(QueryParam.class)) {
                result.query.put(p.getAnnotation(QueryParam.class).value(), value);
                continue;
            }

            if (p.isAnnotationPresent(HeaderParam.class)) {
                result.header.put(p.getAnnotation(HeaderParam.class).value(), value);
                continue;
            }

            // SKIP framework/context params
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
    // version to support rules ---------------- Helpers ----------------

    private boolean matchesRule(List<Entitlement> entitlements,
                                String rule,
                                Map<String, String> pathParams,
                                String securedEndpointId,
                                Method method) {
        try {
            var resolvedRule = rule;

            for (Map.Entry<String, String> entry : pathParams.entrySet()) {
                resolvedRule = resolvedRule.replace(
                        "{" + entry.getKey() + "}",
                        escapeRegexChars(entry.getValue())
                );
            }

            var pattern = Pattern.compile(resolvedRule);

            return entitlements.stream()
                    .anyMatch(e ->

                            pattern.matcher(e.getRaw()).find());


        } catch (Exception ex) {
            LOG.warn("Skipping rule due to exception: " + ex.getMessage());
            return false;
        }
    }

    private String escapeRegexChars(String value) {
        // Escape only regex meta characters: . \ + * ? [ ^ ] $ ( ) { } = ! < > | : -
        return value.replaceAll("([\\\\.+*?\\[\\]^$(){}=!<>|:-])", "\\\\$1");
    }

    private String generateSecuredEndpointId(String httpMethod, String fullPath) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((httpMethod + fullPath).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String getHttpMethod(Method method) {
        for (var annotation : method.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(HttpMethod.class)) {
                return annotation.annotationType().getAnnotation(HttpMethod.class).value();
            }
        }
        throw new IllegalStateException("No HTTP method annotation found on " + method.getName());
    }

    private String buildFullPath(InvocationContext context, Method method) {
        var classPath = method.getDeclaringClass().getAnnotation(Path.class);
        var methodPath = method.getAnnotation(Path.class);

        var full = "";
        if (classPath != null) full += classPath.value();
        if (methodPath != null) full += methodPath.value();
        return normalizePath(full);
    }

    private String normalizePath(String path) {
        if (!path.startsWith("/")) path = "/" + path;
        return path.replaceAll("//+", "/");
    }

    private String resolveResourceFromPath(String fullPath, String pathId) {
        var segments = fullPath.split("/");
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].equals("{" + pathId + "}")) {
                if (i == 0) throw new IllegalStateException("Cannot resolve resource for " + pathId);
                return capitalize(segments[i - 1].replaceAll("s$", ""));
            }
        }
        throw new IllegalStateException("Path parameter {" + pathId + "} not found in path " + fullPath);
    }

    private String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String readBody(ContainerRequestContext context) {
        try (InputStream is = context.getEntityStream()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // reset stream so JAX-RS can still use it
            context.setEntityStream(
                    new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))
            );

            return body;

        } catch (IOException e) {
            throw new RuntimeException("Cannot read request body", e);
        }
    }
}