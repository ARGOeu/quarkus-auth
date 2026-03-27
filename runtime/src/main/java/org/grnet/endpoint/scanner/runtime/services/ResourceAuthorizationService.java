package org.grnet.endpoint.scanner.runtime.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriInfo;
import org.grnet.endpoint.scanner.runtime.EndpointMetadata;
import org.grnet.endpoint.scanner.runtime.EndpointMetadataHolder;
import org.grnet.endpoint.scanner.runtime.endpoints.PageResource;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorization;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorizationRepository;
import org.grnet.endpoint.scanner.runtime.entities.pagination.Page;
import org.grnet.endpoint.scanner.runtime.entities.pagination.PageQueryImpl;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;

@ApplicationScoped
public class ResourceAuthorizationService {


    @Inject
    ResourceAuthorizationRepository repository;

    @Inject
    EndpointMetadataHolder endpointMetadataHolder;

    public PageResource<EndpointMetadata> getSecuredEndpointsByPage(int page, int size, UriInfo uriInfo) {

        var all = endpointMetadataHolder.getData() == null
                ? List.<EndpointMetadata>of()
                : endpointMetadataHolder.getData();

        var pages = partition(all, size);
        var content = pages.getOrDefault(page, List.of());

        var result = new PageQueryImpl<EndpointMetadata>();
        result.list = content;
        result.index = page;
        result.count = all.size();
        result.size = size;
        result.page = Page.of(page, size);

        return new PageResource<>(result, result.list, uriInfo);
    }

    private <T> Map<Integer, List<T>> partition(List<T> list, int pageSize) {
        return IntStream.iterate(0, i -> i + pageSize)
                .limit((list.size() + pageSize - 1) / pageSize)
                .boxed()
                .collect(toMap(i -> i / pageSize,
                        i -> list.subList(i, min(i + pageSize, list.size()))));
    }

    public List<?> findByEndpointId(String id) {
        return repository.list("id", id);
    }

    public void authorize(ResourceAuthorization re) {

        repository.create(re);
    }
    public List<ResourceAuthorization> findByEndpointSecuredEndpointId(String securedEndpointId) {
        return repository.list("secured_endpoint_id", securedEndpointId);
    }

    public List<ResourceAuthorization> findAllResourcesAuthorization() {
        return repository.findAll();
    }
    public void delete(Long id) {
        repository.delete(id);
    }

    public ResourceAuthorization findById(Long id) {
        return repository.findById(id);
    }

    public void updateRule(Long id, String rule) {

        repository.update(id, rule);
    }
}
