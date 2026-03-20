package org.grnet.endpoint.scanner.runtime.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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

    public List<ResourceAuthorization> findAll() {
        return repository.findAll();
    }

    public List<?> findByEndpointId(String id) {
        return repository.list("id", id);
    }

    public void authorize(ResourceAuthorization re) {

        repository.create(re);
    }
    public List<ResourceAuthorization> findByEndpointsecuredEndpointId(String securedEndpointId) {
        return repository.list("secured_endpoint_id", securedEndpointId);
    }

    public void updateAuthorizations(String endpointId, List<String> requestedRules) {
    }


}
