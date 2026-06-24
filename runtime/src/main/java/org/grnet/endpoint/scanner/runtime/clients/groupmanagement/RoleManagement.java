package org.grnet.endpoint.scanner.runtime.clients.groupmanagement;

import org.grnet.endpoint.scanner.runtime.dtos.CreateRoleRequest;
import org.grnet.endpoint.scanner.runtime.dtos.RoleResponse;

import java.util.List;
import java.util.Map;

/**
 * Represents a role management system that models group management structures as roles.
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
 *
 * <p>This interface provides operations to assign roles to users and to create new roles.
 */
public interface RoleManagement {

    /**
     * Assigns a resource-scoped role to a user, granting them access to a specific resource.
     *
     * <p>Resource roles internally associate the given resource type and resource ID,
     * defining the exact resource the user is permitted to access under this role.
     *
     * @param username the unique identifier of the user to whom the role will be assigned
     * @param role     the name of the role to assign
     * @param resource the type or category of the resource (e.g., "Tenant", "Project")
     * @param id       the unique identifier of the specific resource instance
     */
    void assignResourceRoleToUser(String username, String role, String resource, String id, Map<String, List<String>> attributes);

    /**
     * Assigns a global role to a user.
     *
     * <p>Global roles are general-purpose and apply system-wide. Unlike resource roles,
     * they do not carry any resource-specific information and serve as broad
     * permission groupings.
     *
     * @param username the unique identifier of the user to whom the global role will be assigned
     * @param role     the name of the global role to assign
     */
    void assignGlobalRoleToUser(String username, String role);

    void revokeGlobalRoleFromUser(String memberId, String role);

    void revokeResourceRoleFromUser(String memberId, String role, String resource, String id);


    /**
     * Creates a new role within the role management system.
     *
     * @param roleName the unique name of the role to be created
     */
    void createNewRole(CreateRoleRequest roleName);

    /**
     * Assigns the default "members" role to the specified user. This serves as the entry-point role assignment for new users,
     * granting them base-level membership within the system.
     *
     * @param username the unique identifier of the user to be added to the members group
     */
    void assignUserTheMemberRole(String username);

    /**
     * Retrieves all roles available in the role management system.
     *
     * @return a list of {@link RoleResponse} objects representing all existing roles,
     *         or an empty list if no roles are defined
     */
    List<RoleResponse> getAllRoles();
}
