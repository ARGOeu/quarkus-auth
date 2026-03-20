package org.grnet.endpoint.scanner.runtime.services;

import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.entities.EndpointResolver;
import org.grnet.endpoint.scanner.runtime.entities.EndpointResolverRepository;

import static io.quarkus.arc.ComponentsProvider.LOG;

public class ResolverConfigService {

    @Inject
    EndpointResolverRepository endpointResolverRepository;
    //private final List<ResolverConfig> configs = List.of(new ResolverConfig("aff8b3cc059b8a784e55a131b030c4f3a6489c9dfb93a0ad7895199dfb4e273b", "Tenant", "id", "name"), new ResolverConfig("aff8b3cc059b8a784e55a131b030c4f3a6489c9dfb93a0ad7895199dfb4e273b","User", "id", "email"), new ResolverConfig("aff8b3cc059b8a784e55a131b030c4f3a6489c9dfb93a0ad7895199dfb4e273b","Project", "id", "slug"));

    public String findField(String securedEndpointId, String resource, String pathParam) {

        var configs = endpointResolverRepository.list("secured_endpoint_id", securedEndpointId);

        var field = configs.stream()
                .filter(c -> {
                    boolean matchesResource = c.getResource().equals(resource);
                    boolean matchesField = c.getOriginalField().equals(pathParam);
                    return matchesResource && matchesField;
                })
                .map(c -> {
                    return c.getMappedField();
                })
                .findFirst()
                .orElseThrow(() -> {
                    String msg = "No resolver config found for " + resource + ":" + pathParam;
                    LOG.warn(msg);
                    return new IllegalStateException(msg);
                });
       return field;


    }
//
//    public class ResolverConfig {
//        public String securedEndpointId;
//
//        public String resource;
//        public String pathParam;
//        public String field;
//
//        public ResolverConfig(String securedEndpointId, String resource, String pathParam, String field) {
//            this.securedEndpointId = securedEndpointId;
//            this.resource = resource;
//            this.pathParam = pathParam;
//            this.field = field;
//        }
//    }
}
