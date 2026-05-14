package org.grnet.endpoint.scanner.runtime.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.grnet.endpoint.scanner.runtime.ApiResourceHolder;
import org.grnet.endpoint.scanner.runtime.ApiResourceMetadata;
import org.grnet.endpoint.scanner.runtime.EndpointMetadata;
import org.grnet.endpoint.scanner.runtime.EndpointMetadataHolder;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.AuthGroupManagement;
import org.grnet.endpoint.scanner.runtime.endpoints.AssignRoleRequest;
import org.grnet.endpoint.scanner.runtime.endpoints.CreateRoleRequest;
import org.grnet.endpoint.scanner.runtime.endpoints.InformativeResponse;
import org.grnet.endpoint.scanner.runtime.endpoints.PageResource;
import org.grnet.endpoint.scanner.runtime.endpoints.RoleResponse;
import org.grnet.endpoint.scanner.runtime.endpoints.UserProfileDto;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorization;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorizationRepository;
import org.grnet.endpoint.scanner.runtime.entities.pagination.Page;
import org.grnet.endpoint.scanner.runtime.entities.pagination.PageQueryImpl;
import org.grnet.endpoint.scanner.runtime.process.AfterProcessing;
import org.grnet.endpoint.scanner.runtime.process.BeforeProcessing;
import org.grnet.endpoint.scanner.runtime.process.Event;

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

    @Inject
    ApiResourceHolder apiResourceHolder;

    @Inject
    AuthGroupManagement authGroupManagement;

    @Inject
    jakarta.enterprise.event.Event<Event> event;

    @Inject
    Utility utility;

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

    public PageResource<ApiResourceMetadata> getApiResourcesByPage(int page, int size, UriInfo uriInfo) {

        var all = apiResourceHolder.getData() == null
                ? List.<ApiResourceMetadata>of()
                : apiResourceHolder.getData();

        var pages = partition(all, size);
        var content = pages.getOrDefault(page, List.of());

        var result = new PageQueryImpl<ApiResourceMetadata>();
        result.list = content;
        result.index = page;
        result.count = all.size();
        result.size = size;
        result.page = Page.of(page, size);

        return new PageResource<>(result, result.list, uriInfo);
    }

    public List<RoleResponse> getAllRoles() {

        return authGroupManagement.getAllRoles();

     }

    public PageResource<RoleResponse> getAllRolesByPageAndSize(int page, int size, UriInfo uriInfo) {

        var all = authGroupManagement.getAllRoles();

        var pages = partition(all, size);
        var content = pages.getOrDefault(page, List.of());

        var result = new PageQueryImpl<RoleResponse>();
        result.list = content;
        result.index = page;
        result.count = all.size();
        result.size = size;
        result.page = Page.of(page, size);

        return new PageResource<>(result, result.list, uriInfo);
    }

    public Object assignRoleToUser(AssignRoleRequest request){

        var roleEvent = new Event(request.extras);

        event.select(new BeforeProcessing.Literal("assign-role")).fire(roleEvent);

        if (!roleEvent.isSkipDefault()) {
            roleEvent.setResult(defaultLogic(request));
        }

        event.select(new AfterProcessing.Literal("assign-role")).fire(roleEvent);

        return roleEvent.getResult();
    }

    public void createNewRole(CreateRoleRequest request){

        authGroupManagement.createNewRole(request.name);
    }

    private InformativeResponse defaultLogic(AssignRoleRequest request){

        var response = new InformativeResponse();
        response.code = 200;
        response.message = "Role assigned successfully!";

        if(StringUtils.isEmpty(request.apiResource)){

            authGroupManagement.assignGlobalRoleToUser(request.username, request.role);
        } else if(StringUtils.isNotEmpty(request.apiResource) && StringUtils.isEmpty(request.resourceId)) {

            throw new BadRequestException("api_resource exists and resource_id is empty!");
        } else {

//            apiResourceHolder
//                    .getData()
//                    .stream()
//                    .filter(r->r.getSimpleName().equals(request.apiResource))
//                    .findAny()
//                    .orElseThrow(()->new NotFoundException(request.apiResource+" not found!"));


            apiResourceHolder.getData()
                    .stream()
                    .filter(r -> r.getResourceName().equals(request.apiResource))
                    .findAny()
                    .orElseThrow(() -> new NotFoundException(request.apiResource + " not found!"));
            authGroupManagement.assignResourceRoleToUser(request.username, request.role, request.apiResource, request.resourceId);
        }

        return response;
    }

    public UserProfileDto getUserProfile() {

        var userProfile = new UserProfileDto();

        userProfile.id = utility.getUserUniqueIdentifier();
        userProfile.username = utility.getUsername();
        userProfile.email = utility.getUserEmail();
        userProfile.name = utility.getUserName();
        userProfile.surname = utility.getUserSurname();
        //userProfile.groups = userEntitlementsService.getUserEntitlements();

        return userProfile;
    }

    public void assignUserTheMemberRole() {

        authGroupManagement.assignUserTheMemberRole(utility.getUsername());
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
