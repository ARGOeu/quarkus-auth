package org.grnet.endpoint.scanner.runtime;

//import io.quarkus.logging.Log;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.grnet.endpoint.scanner.runtime.entitlements.EntitlementProvider;
import org.grnet.endpoint.scanner.runtime.resolvers.GroupIdResolver;
import org.grnet.endpoint.scanner.runtime.resolvers.TestGroupIdResolver;
import org.grnet.endpoint.scanner.runtime.resolvers.TestResolver;
import org.grnet.endpoint.scanner.runtime.services.ResourceAuthorizationService;
import org.jboss.logging.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
    private static final Logger LOG = Logger.getLogger(SecuredEndpointInterceptor.class);

    @AroundInvoke
    public Object checkAccess(InvocationContext context) throws Exception {

        var entitlements = entitlementProvider.fetchEntitlements();

        if(entitlementProvider.isSuperAdmin(config)){
            return context.proceed();
        }

        // 2️⃣ Determine HTTP method and full path
        var method = context.getMethod(); // Java reflection Method
        var httpMethod = getHttpMethod(method); // e.g., GET, POST, etc.
        var fullPath = buildFullPath(context, method); // combine class + method @Path

        // 3️⃣ Generate securedEndpointId hash
        String securedEndpointId = generateSecuredEndpointId(httpMethod, fullPath);

        // 4️⃣ Retrieve ResourceAuthorization from DB
        var authList = resourceAuthorizationService.findByEndpointsecuredEndpointId(securedEndpointId);
        if (authList.isEmpty()) {
           // Log.warn("No ResourceAuthorization found for securedEndpointId: " + securedEndpointId);
            throw new ForbiddenException("You cannot access this resource!");
        }

        // 6️⃣ Get tenantId & topologyId from method args
        MultivaluedMap<String, String> pathParams = uriInfo.getPathParameters();

        boolean hasAccess = entitlements.stream()
                .anyMatch(e -> authList.stream()
                        .anyMatch(ra -> {
                            try {
                                var resolvedRegex = resolveRegex(securedEndpointId, method, ra.getRule());
                                // Replace placeholders
                                for (Map.Entry<String, List<String>> entry : pathParams.entrySet()) {

                                    String key = entry.getKey();
                                    String value = entry.getValue().get(0);

                                    if (resolvedRegex.contains("{" + key + "}")) {
                                        resolvedRegex = resolvedRegex.replace("{" + key + "}", value);
                                    }
                                }

                                // Escape braces
                                String escapedRegex = resolvedRegex.replace("{", "\\{").replace("}", "\\}");
                                // Compile pattern and match
                                Pattern pattern = Pattern.compile(escapedRegex);
                                var matches=pattern.matcher(e.getRaw()).matches();
                                return pattern.matcher(e.getRaw()).matches();



                            } catch (Exception ex) {
                                // Skip this regex if it fails
                         //       Log.warn("Skipping regex for  " + ra.getName() + " due to exception: " + ex.getMessage());
                                return false; // continue to next ra
                            }
                        })
                );

        if (!hasAccess) {
            throw new ForbiddenException("You cannot access this resource!");
        }

        return context.proceed();
    }

    // --- Helpers ---
    private String generateSecuredEndpointId(String httpMethod, String fullPath) {
        var raw = httpMethod + fullPath;
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate securedEndpointId hash", e);
        }
    }

    private String getHttpMethod(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(HttpMethod.class)) {
                return annotation.annotationType().getAnnotation(HttpMethod.class).value();
            }
        }
        throw new IllegalStateException("No HTTP method annotation found on " + method.getName());
    }

    private String buildFullPath(InvocationContext context, Method method) {
        Path classPath = method.getDeclaringClass().getAnnotation(Path.class);
        Path methodPath = method.getAnnotation(Path.class);

        String full = "";
        if (classPath != null) full += classPath.value();
        if (methodPath != null) full += methodPath.value();

        return normalizePath(full);
    }

    private String normalizePath(String path) {
        // Ensure single leading slash and no double slashes
        if (!path.startsWith("/")) path = "/" + path;
        path = path.replaceAll("//+", "/");
        return path;
    }

    private String resolveRegex(String securedEndpointId, Method method, String regex) {
        SecuredEndpoint endpoint = method.getAnnotation(SecuredEndpoint.class);
        TestResolver[] resolverDefs = endpoint.resolvers();


        for (TestResolver resolverDef : resolverDefs) {

            Class<? extends TestGroupIdResolver> resolverClass = resolverDef.idResolver();
            String pathId = resolverDef.pathId();

            TestGroupIdResolver resolver = resolverInstances.select(resolverClass).get();

            //String resolvedValue = resolver.resolve(pathId);
            List<EndpointMetadata> endpoints = endpointMetadataHolder.getData();
            String resource = endpoints.stream()
                    .filter(e -> e.getSecuredEndpointId().equals(securedEndpointId))
                    .findFirst()
                    .map(e -> {
                        String r = resolveResourceFromPath(e.getPath(), pathId);
                        return r;
                    })
                    .orElseThrow(() -> new IllegalStateException(
                            "No endpoint metadata found for securedEndpointId " + securedEndpointId));

            String resolvedValue = resolver.resolve(securedEndpointId,resource, pathId);


            var replacedRegex = regex.replace("{" + pathId + "}", resolvedValue);
            return replacedRegex;
        }
        return regex;
    }

    private String resolveResourceFromPath(String fullPath, String pathId) {

        String[] segments = fullPath.split("/");

        for (int i = 0; i < segments.length; i++) {

            if (segments[i].equals("{" + pathId + "}")) {

                if (i == 0) {
                    throw new IllegalStateException("Cannot resolve resource for " + pathId);
                }

                String resourceSegment = segments[i - 1];

                // normalize: tenants -> Tenant
                return capitalize(resourceSegment.replaceAll("s$", ""));
            }
        }

        throw new IllegalStateException("Path parameter {" + pathId + "} not found in path " + fullPath);
    }

    private String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}