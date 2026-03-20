package org.grnet.endpoint.scanner.runtime;

import org.grnet.endpoint.scanner.runtime.resolvers.RepositoryRegistry;

public interface RepositoryRegistrar {
    void registerRepositories(RepositoryRegistry registry);
}