package org.grnet.endpoint.scanner.runtime.resolvers;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.grnet.endpoint.scanner.runtime.services.ResolverConfigService;


public class DynamicResolver implements TestGroupIdResolver {

    @Inject
    RepositoryRegistry repositoryRegistry;

    @Inject
    ResolverConfigService resolverConfigService;

    @Context
    UriInfo uriInfo;


    @Override
    public String resolve(String securedEndpointId, String resource, String pathId) {

        String rawId = uriInfo.getPathParameters().getFirst(pathId);
        if (rawId == null || rawId.isBlank()) {
            throw new BadRequestException("Missing required path parameter '" + pathId + "'");
        }

        // Get which field to use dynamically
        String field = resolverConfigService.findField(securedEndpointId,resource, pathId);
        // Get entity dynamically
        Object entity = repositoryRegistry.findEntity(resource, rawId);

        // Return field dynamically via reflection
        try {
            java.lang.reflect.Field f = entity.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return String.valueOf(f.get(entity));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "Unable to resolve field " + field + " for resource " + resource, e);
        }
    }

    @Override
    public String resolve(String subgroupValue) {
        return "";
    }
}
