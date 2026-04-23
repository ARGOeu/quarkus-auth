package org.grnet.endpoint.scanner.runtime;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.UriInfo;
import org.grnet.endpoint.scanner.runtime.entitlements.Entitlement;
import org.grnet.endpoint.scanner.runtime.entitlements.EntitlementProvider;
import org.grnet.endpoint.scanner.runtime.resolvers.GroupIdResolver;
import org.grnet.endpoint.scanner.runtime.services.ResourceAuthorizationService;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Interceptor
@SecuredEndpoint
@Priority(10)
public class SecuredEndpointInterceptor {

    @Inject
    EntitlementProvider entitlementProvider;
    @Inject
    UriInfo uriInfo;
    @Inject
    EndpointMetadataHolder endpointMetadataHolder;
    @Inject
    ResourceAuthorizationService resourceAuthorizationService;

    @Inject
    Instance<GroupIdResolver> resolverInstances;
    @Inject
    SecuredEndpointConfig config;

    @Inject
    ResourceRepositoryMetadataHolder resourceRepositoryMetadataHolder;

    private static final Logger LOG = Logger.getLogger(SecuredEndpointInterceptor.class);

    @AroundInvoke
    public Object checkAccess(InvocationContext context) throws Exception {

        var entitlements = entitlementProvider.fetchEntitlements();

        if (entitlementProvider.isSuperAdmin(config)) {
            return context.proceed();
        }

        var method = context.getMethod();
        var httpMethod = getHttpMethod(method);
        var fullPath = buildFullPath(context, method);

        var securedEndpointId = generateSecuredEndpointId(httpMethod, fullPath);

        var authList = resourceAuthorizationService.findByEndpointSecuredEndpointId(securedEndpointId);
        if (authList.isEmpty()) {
            throw new ForbiddenException("You cannot access this resource!");
        }

        // Map of request path params
        var pathParams = uriInfo.getPathParameters()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get(0)
                ));

        boolean hasAccess = authList.stream()
                .anyMatch(ra -> matchesRule(entitlements, ra.getRule(), pathParams, securedEndpointId, method));

        if (!hasAccess) {
            throw new ForbiddenException("You cannot access this resource!");
        }

        return context.proceed();
    }

    // ---------------- Helpers ----------------

    private boolean matchesRule(List<Entitlement> entitlements,
                                String rule,
                                Map<String, String> pathParams,
                                String securedEndpointId,
                                Method method) {
        try {
            var resolvedRule = rule;

//            // 1️⃣ Resolve dynamic placeholders via resolvers
//            if (resolvedRule.contains("{")) {
//                resolvedRule = resolveRegex(securedEndpointId, method, resolvedRule);
//            }

            // 2️⃣ Replace path parameters with actual values (escaped for regex)
                for (Map.Entry<String, String> entry : pathParams.entrySet()) {
                    resolvedRule = resolvedRule.replace(
                            "{" + entry.getKey() + "}",
                            escapeRegexChars(entry.getValue())
                    );
                }

            // 3️⃣ If no regex wildcards, escape rule for literal match
//            if (!resolvedRule.contains(".*") && !resolvedRule.contains("^") && !resolvedRule.contains("$")) {
//                resolvedRule = Pattern.quote(resolvedRule);
//            }

            var pattern = Pattern.compile(resolvedRule);

            // 4️⃣ Match against all entitlements
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

//    private String resolveRegex(String securedEndpointId, Method method, String regex) {
//        var endpoint = method.getAnnotation(SecuredEndpoint.class);
//        var resolverDefs = endpoint.resolvers();
//
//        var resolvedRule = regex;
//        for (TestResolver resolverDef : resolverDefs) {
//            Class<? extends TestGroupIdResolver> resolverClass = resolverDef.idResolver();
//            String pathId = resolverDef.pathId();
//
//            TestGroupIdResolver resolver = resolverInstances.select(resolverClass).get();
//            List<EndpointMetadata> endpoints = endpointMetadataHolder.getData();
//            String resource = endpoints.stream()
//                    .filter(e -> e.getSecuredEndpointId().equals(securedEndpointId))
//                    .findFirst()
//                    .map(e -> resolveResourceFromPath(e.getPath(), pathId))
//                    .orElseThrow(() -> new IllegalStateException(
//                            "No endpoint metadata found for securedEndpointId " + securedEndpointId));
//
//            String resolvedValue = resolver.resolve(securedEndpointId, resource, pathId);
//            resolvedRule = resolvedRule.replace("{" + pathId + "}", resolvedValue);
//        }
//        return resolvedRule;
//    }

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
}