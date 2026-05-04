package org.grnet.endpoint.scanner.runtime.clients.groupmanagement;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response.AddGroupMemberRequest;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response.Group;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response.GroupMembersResponse;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response.GroupMembership;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response.GroupRequest;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response.GroupResponse;

@Path("/agm/account")
public interface KeycloakGroupManagementClient {

    // -------------------------------------------------------------
    // Fetch groups (search root hierarchy)
    // -------------------------------------------------------------
    @GET
    @Path("/group-admin/groups")
    GroupResponse getGroups(@QueryParam("search") String search);

    // -------------------------------------------------------------
    // Create subgroup
    // -------------------------------------------------------------
    @POST
    @Path("/group-admin/group/{id}/children")
    @Consumes(MediaType.APPLICATION_JSON)
    void createSubGroup(@PathParam("id") String parentId, GroupRequest request);

    @GET
    @Path("/group-admin/group/{groupId}/members")
    @Produces(MediaType.APPLICATION_JSON)
    GroupMembersResponse getGroupMembers(@PathParam("groupId") String groupId, @QueryParam("first") int first, @QueryParam("max") int max, @QueryParam("search") String search);

    @GET
    @Path("/group-admin/group/{groupId}/members")
    @Produces(MediaType.APPLICATION_JSON)
    GroupMembersResponse getMembersByRole(@PathParam("groupId") String groupId, @QueryParam("role") String role);

    // -------------------------------------------------------------
    // Get group
    // -------------------------------------------------------------
    @GET
    @Path("/group-admin/group/{id}/all")
    Group getGroup(@PathParam("id") String id);

    // -------------------------------------------------------------
    // Delete group
    // -------------------------------------------------------------
    @DELETE
    @Path("/group-admin/group/{id}")
    void deleteGroup(@PathParam("id") String id);

    // -------------------------------------------------------------
    // Add role to group
    // -------------------------------------------------------------
    @POST
    @Path("/group-admin/group/{id}/roles")
    void addRole(@PathParam("id") String id, @QueryParam("name") String role);


    // -------------------------------------------------------------
    // Add user to group
    // -------------------------------------------------------------
    @POST
    @Path("/group-admin/group/{groupId}/members")
    @Consumes(MediaType.APPLICATION_JSON)
    String addUserToGroup(@PathParam("groupId") String groupId, AddGroupMemberRequest body);

    // -------------------------------------------------------------
    // Update configuration (default config / roles)
    // -------------------------------------------------------------
    @POST
    @Path("/group-admin/group/{id}/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    void updateConfiguration(@PathParam("id") String id, GroupMembership config);

    // -------------------------------------------------------------
    // Get configuration (extended Keycloak)
    // -------------------------------------------------------------
    @GET
    @Path("/group-admin/group/{groupId}/configuration/{configId}")
    GroupMembership getConfiguration(@PathParam("groupId") String groupId,
                                     @PathParam("configId") String configId);

    @DELETE
    @Path("/group-admin/group/{groupId}/member/user/{memberId}")
    void removeMemberFromGroup(@PathParam("groupId") String groupId,
                               @PathParam("memberId") String memberId);
}
