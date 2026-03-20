package org.grnet.endpoint.scanner.runtime.resolvers;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NoOpTestResolver implements TestGroupIdResolver {
    @Override
    public String resolve(String subgroupValue) {
        return subgroupValue;
    }

    @Override
    public String resolve(String securedEndpointId,String resource, String pathId) {
        return resource;
    }
}
