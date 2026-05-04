package org.grnet.endpoint.scanner.runtime.clients.groupmanagement;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response.AddGroupMemberRequest;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response.Group;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response.GroupMembersResponse;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response.GroupRequest;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response.GroupUser;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response.PartialGroup;
import org.grnet.endpoint.scanner.runtime.endpoints.RoleResponse;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation of {@link RoleManagement} that models group management structures as roles
 * using the Auth Group Management system.
 * Each group beneath a parent group functions as a role within this system.
 *
 * <p>There are two types of roles:
 * <ul>
 *   <li><b>Global Roles</b> - General-purpose roles that apply system-wide and do not
 *       contain any additional resource-specific information.</li>
 *   <li><b>Resource Roles</b> - Roles that are scoped to specific resources. Internally,
 *       they encapsulate the resource type and its associated ID, defining what a user
 *       has access to.</li>
 * </ul>
 */
public class AuthGroupManagement implements GroupManagement, RoleManagement {

    private static final Logger LOG = Logger.getLogger(AuthGroupManagement.class);

    private final KeycloakGroupManagementClient groupClient;

    private final String parentGroup;

    public AuthGroupManagement(String url, BearerTokenRequestFilter filter, String parentGroup) {
        this.parentGroup = parentGroup;
        this.groupClient = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(url))
                .register(KeycloakExceptionMapper.class)
                .register(filter)
                .build(KeycloakGroupManagementClient.class);
    }

    // ---------------------------------------------------------
    // CREATE GROUP
    // ---------------------------------------------------------
    @Override
    public void createGroup(String parentPath, String name, List<String> roles, Map<String, List<String>> attributes) {

        if (roles == null) roles = List.of();
        if (attributes == null) attributes = Map.of();

        // Build request
        var req = new GroupRequest();
        req.name = name;
        req.attributes = attributes;


        // Resolve parent group ID
        var parentId = getGroupIdByPath(parentPath);
        if (parentId == null)
            throw new IllegalStateException("Creating group failed. Parent group not found at path: " + parentPath);

        // Create child group
        groupClient.createSubGroup(parentId, req);

        // Resolve new group ID (path-based)
        var newGroupPath = parentPath + "/" + name;
        var newGroupId = getGroupIdByPath(newGroupPath);
        if (newGroupId == null)
            throw new IllegalStateException("Creating group failed. New group not found after creation: " + newGroupPath);

        LOG.infof("Group created: %s → ID: %s", newGroupPath, newGroupId);

        if(!roles.isEmpty()){

            // Assign roles
            for (String role : roles) {
                groupClient.addRole(newGroupId, role);
            }

            // Update the group configuration with roles
            updateGroupConfigurationRoles(newGroupId, roles);
        }
    }

    // ---------------------------------------------------------
    // DELETE GROUP
    // ---------------------------------------------------------
    @Override
    public void deleteGroup(String fullGroupPath) {
        try {
            var id = getGroupIdByPath(fullGroupPath);
            if (id != null) {
                groupClient.deleteGroup(id);
            } else {
                LOG.warnf("Group not found for deletion: %s", fullGroupPath);
            }
        } catch (Exception e) {
            LOG.errorf("Failed to delete group %s: %s", fullGroupPath, e.getMessage());
        }
    }

    /**
     * Creates a new group within the group management system.
     *
     * The created role represents a group beneath the parent group in the
     * group management structure. Once created, the role can be assigned to users
     * either as a global role or configured as a resource role with associated resources.
     *
     * @param roleName the unique name of the role to be created
     */
    @Override
    public void createNewRole(String roleName){

        var parentPath = "/" + parentGroup;
        createGroup(parentPath, roleName, List.of(), null);
    }

    /**
     * Assigns the default "members" role to the specified user,
     * effectively adding them to the members group in the Group Management system.
     *
     * <p>This serves as the entry-point role assignment for new users,
     * granting them base-level membership within the system.
     *
     * @param username the unique identifier of the user to be added to the members group
     */
    @Override
    public void assignUserTheMemberRole(String username) {

        var fullPath = "/" + parentGroup + "/members";

        var groupId = getGroupIdByPath(fullPath);

        var response = groupClient.getGroupMembers(groupId, 0, 10, username);

        if(response.count>0){
            return;
        }

        groupClient.addUserToGroup(groupId, new AddGroupMemberRequest(username, List.of("member")));
    }

    @Override
    public GroupMembersResponse fetchGroupMembers(String fullPath, int first, int max, String search) {

        var groupId = getGroupIdByPath(fullPath);

        return groupClient.getGroupMembers(groupId, first, max, search);
    }

    /**
     * Retrieves all groups under the parent group available in the group management system.
     *
     * @return a list of {@link RoleResponse} objects representing all existing roles,
     *         or an empty list if no roles are defined
     */
    @Override
    public List<RoleResponse> getAllRoles(){

        var parentGroupId = "/" + parentGroup;

        var groupId = getGroupIdByPath(parentGroupId);

        var group = groupClient.getGroup(groupId);

        return group.extraSubGroups.stream().map(gr -> new RoleResponse(gr.id, gr.name)).collect(Collectors.toList());
    }

    // ---------------------------------------------------------
    // ADD ROLE
    // ---------------------------------------------------------
    @Override
    public void addRole(String groupId, String role) {
        groupClient.addRole(groupId, role);
    }

    // ---------------------------------------------------------
    // UPDATE CONFIGURATION (ROLES)
    // ---------------------------------------------------------
    @Override
    public void updateConfiguration(String groupId, List<String> roles) {
        updateGroupConfigurationRoles(groupId, roles);
    }

    @Override
    public List<GroupUser> fetchGroupMembersByRole(String fullPath, String role) {

        var groupId = getGroupIdByPath(fullPath);

        LOG.infof("AGM getMembersByRole fullPath=%s groupId=%s role=%s", fullPath, groupId, role);


        var response = groupClient.getMembersByRole(groupId, role);

        if (response == null || response.results == null) {
            return List.of();
        }

        return response.results.stream()
                .map(entry -> entry.user)
                .toList();
    }

    @Override
    public void addGroupMember(String fullPath, String username, String role) {

        var groupId = getGroupIdByPath(fullPath);

        var response = groupClient.getGroupMembers(groupId, 0, 10, username);

        if(response.count>0){
            return;
        }

        groupClient.addUserToGroup(groupId, new AddGroupMemberRequest(username, List.of(role)));
    }

    @Override
    public void assignGlobalRoleToUser(String username, String role){

        var parentPath = "/" + parentGroup + "/" + role;

        addGroupMember(parentPath, username, "member");
    }

    @Override
    public void assignResourceRoleToUser(String username, String role, String resource, String id){

        var specificResourcePath = "/" + parentGroup + "/" + role + "/"+ resource+"/"+id;

        var specificResourceGroupId = getGroupIdByPath(specificResourcePath);

        if (Objects.isNull(specificResourceGroupId)){

            var resourcePath = "/" + parentGroup + "/" + role + "/"+ resource;

            var resourceGroupId = getGroupIdByPath(resourcePath);

            if(Objects.isNull(resourceGroupId)){

                createGroup("/" + parentGroup + "/" + role, resource, List.of(), null);
            }

            createGroup("/" + parentGroup + "/" + role + "/"+ resource, id, List.of(), null);
        }

        addGroupMember(specificResourcePath, username, "member");
    }

    @Override
    public void addMemberToGroupByGroupId(String id, String username, String role) {

        var response = groupClient.getGroupMembers(id, 0, 10, username);

        if (response.count>0) {
            return;
        }

        groupClient.addUserToGroup(id, new AddGroupMemberRequest(username, List.of(role)));
    }

    private void updateGroupConfigurationRoles(String groupId, List<String> roles) {

        // Fetch full group structure
        var group = groupClient.getGroup(groupId);

        // Obtain defaultConfiguration ID
        var configId = group.attributes
                .get("defaultConfiguration")
                .get(0);

        // Fetch complete configuration JSON object
        var config = groupClient.getConfiguration(groupId, configId);

        // Assign new roles
        config.setGroupRoles(roles);

        // Update configuration
        groupClient.updateConfiguration(groupId, config);

        LOG.infof("Updated roles for group %s → %s", groupId, roles);
    }

    // ---------------------------------------------------------
    // GROUP LOOKUP UTILITIES
    // ---------------------------------------------------------
    @Override
    public String getGroupId(String fullPath) {
        return getGroupIdByPath(fullPath);
    }

    // Internal lookup: resolves a group ID from the flattened groups map
    private String getGroupIdByPath(String fullPath) {
        return flattenGroups().get(fullPath);
    }

    // Builds a map of all groups (path → id, id → defaultConfigId) by flattening the Keycloak tree
    private Map<String, String> flattenGroups() {
        var response = groupClient.getGroups("");
        Map<String, String> map = new HashMap<>();

        for (Group group : response.results) {
            collectGroupRecursive(group, map);
        }
        return map;
    }

    @Override
    public List<PartialGroup> fetchGroups() {

        var groups = new ArrayList<PartialGroup>();

        var response = groupClient.getGroups("");

        for (Group group : response.results) {
            collectGroupRecursive(group, groups);
        }
        return groups;
    }

    @Override
    public void removeMemberFromGroup(String fullPath, String memberId) {

        var groupId = getGroupIdByPath(fullPath);

        groupClient.removeMemberFromGroup(groupId, memberId);
    }

    // Recursively adds a group's path, id, and default configuration to the lookup map
    private void collectGroupRecursive(Group group, Map<String, String> map) {
        // Path → ID
        map.put(group.path, group.id);

        // ID → defaultConfiguration
        if (group.attributes != null && group.attributes.containsKey("defaultConfiguration")) {
            map.put(group.id, group.attributes.get("defaultConfiguration").get(0));
        }

        if (group.extraSubGroups != null) {
            for (Group child : group.extraSubGroups) {
                collectGroupRecursive(child, map);
            }
        }
    }
}

