package org.grnet.endpoint.scanner.runtime.resolvers;

public interface TestGroupIdResolver extends GroupIdResolver {
    String resolve(String securedEndpointId,String resource, String pathId);
}
