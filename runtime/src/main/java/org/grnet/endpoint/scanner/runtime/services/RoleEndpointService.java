package org.grnet.endpoint.scanner.runtime.services;


import io.undertow.util.BadRequestException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.grnet.endpoint.scanner.runtime.Scope;
import org.grnet.endpoint.scanner.runtime.SecuredEndpoint;
import org.grnet.endpoint.scanner.runtime.dtos.*;
import org.grnet.endpoint.scanner.runtime.entities.RoleEndpoint;
import org.grnet.endpoint.scanner.runtime.repositories.RoleEndpointRepository;
import org.jboss.resteasy.reactive.server.util.ScoreSystem;
;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class RoleEndpointService {

    @Inject
    RoleEndpointRepository roleEndpointRepository;
    @Inject
    ResourceAuthorizationService resourceAuthorizationService;

    public RoleEndpointAssignmentResponse getAssignedEndpoints() {

        var roleEndpoints = roleEndpointRepository.findAll();

        var grouped = roleEndpoints.stream()
                        .collect(Collectors.groupingBy(RoleEndpoint::getRoleId));

        var assignments = grouped.entrySet().stream()
                .map(entry -> {

                var group = entry.getValue();
                var first = group.get(0);

                var ra = new RoleEndpointAssignmentResponse.RoleAssignment();

                ra.setRoleId(entry.getKey());
                ra.setRoleName(first.getRoleName());

                var endpoints = group.stream()
                        .collect(Collectors.groupingBy(RoleEndpoint::getSecuredEndpointId))
                        .entrySet()
                        .stream()
                        .map(e -> {
                            var dto = new SecuredEndpointAssignment();

                            dto.setSecuredEndpointId(e.getKey());

                            var scopes = e.getValue().stream()
                                    .map(RoleEndpoint::getScope)
                                    .filter(s -> s != null && !s.isBlank())
                                    .map(String::trim)
                                    .distinct()
                                    .map(scopeStr -> {
                                        try {
                                            return Scope.valueOf(scopeStr);
                                        } catch (IllegalArgumentException ex) {
                                            return null;
                                        }
                                    })
                                    .filter(Objects::nonNull)
                                    .toList();

                            dto.setScope(
                                    scopes.stream()
                                            .findFirst()
                                            .orElse(null)
                            );

                            return dto;
                        })
                        .toList();

                ra.setSecuredEndpoints(endpoints);

                return ra;
                })
                .toList();

        var response = new RoleEndpointAssignmentResponse();
        response.setAssignments(assignments);

        return response;
    }

    public RoleEndpointAssignmentResponse getAssignedEndpointsByRoleId(String roleId) {

        var roleEndpoints = roleEndpointRepository.findAll()
                .stream()
                .filter(re -> re.getRoleId().equals(roleId))
                .toList();

        var response = new RoleEndpointAssignmentResponse();

        if (roleEndpoints.isEmpty()) {
            response.setAssignments(Collections.emptyList());
            return response;
        }

        var first = roleEndpoints.get(0);

        var assignment = new RoleEndpointAssignmentResponse.RoleAssignment();

        assignment.setRoleId(roleId);
        assignment.setRoleName(first.getRoleName());

        assignment.setSecuredEndpoints(
                roleEndpoints.stream()
                        .collect(Collectors.groupingBy(RoleEndpoint::getSecuredEndpointId))
                        .entrySet()
                        .stream()
                        .map(entry -> {

                            var dto = new SecuredEndpointAssignment();

                            dto.setSecuredEndpointId(entry.getKey());

                            // collect scope(s) for this endpoint
                            var scopes =
                                    entry.getValue().stream()
                                            .map(RoleEndpoint::getScope)
                                            .filter(s -> s != null && !s.isBlank())
                                            .map(String::trim)
                                            .distinct()
                                            .map(scopeStr -> {
                                                try {
                                                    return Scope.valueOf(scopeStr);
                                                } catch (IllegalArgumentException e) {
                                                    return null; // or log it
                                                }
                                            })
                                            .filter(Objects::nonNull)
                                            .toList();
                            dto.setScope(scopes.isEmpty() ? null : scopes.get(0));
                            return dto;
                        })
                        .collect(Collectors.toList())
        );
        response.setAssignments(List.of(assignment));

        return response;
    }

    @Transactional
    public void assignRolesToEndpoints(RoleEndpointAssignmentRequest request) {

        var existing = roleEndpointRepository.findAll();

        var requestedRoleIds = request.getAssignments()
                .stream()
                .map(RoleEndpointAssignmentRequest.RoleAssignment::getRoleId)
                .collect(Collectors.toSet());

        var dbRoleIds = existing.stream()
                .map(RoleEndpoint::getRoleId)
                .collect(Collectors.toSet());

        // DELETE roles not in request
        var rolesToDelete = new HashSet<>(dbRoleIds);
        rolesToDelete.removeAll(requestedRoleIds);

        for (String roleId : rolesToDelete) {
            roleEndpointRepository.deleteByRoleId(roleId);
        }

        //  SYNC remaining roles
        for (var assignment : request.getAssignments()) {

            var roleId = assignment.getRoleId();
            var roleName = assignment.getRoleName();

            var incoming = new HashSet<>(assignment.getSecuredEndpointIds());

            var roleEntries = roleEndpointRepository.list("role_id", roleId);

            var existingEndpoints = roleEntries.stream()
                    .map(RoleEndpoint::getSecuredEndpointId)
                    .collect(Collectors.toSet());

            // delete removed endpoints
            var toDelete = new HashSet<>(existingEndpoints);
            toDelete.removeAll(incoming);

            if (!toDelete.isEmpty()) {
                roleEndpointRepository.deleteByRoleIdAndEndpointIds(roleId, new ArrayList<>(toDelete));
            }

            // insert new endpoints
            var toInsert = new HashSet<>(incoming);
            toInsert.removeAll(existingEndpoints);

            for (String endpointId : toInsert) {
                RoleEndpoint re = new RoleEndpoint();
                re.setRoleId(roleId);
                re.setRoleName(roleName);
                re.setSecuredEndpointId(endpointId);
                roleEndpointRepository.create(re);
            }
        }
    }

    @Transactional
    public void assignRolesToEndpointsPerRole(String roleId, SecuredEndpointPerRoleRequest request) {

        var role = resourceAuthorizationService.getAllRoles().stream()
                .filter(r -> r.id.equals(roleId))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Role not found: " + roleId)
                );

        var existing = roleEndpointRepository.list("role_id", roleId);

        // key = endpointId:scope
        var existingMap = existing.stream()
                .collect(Collectors.toMap(
                        e -> e.getSecuredEndpointId()
                                + ":" +
                                (e.getScope() != null
                                        ? e.getScope().toLowerCase()
                                        : ""),
                        e -> e,
                        (a, b) -> a
                ));

        // incoming unique keys
        var incomingKeys = request.getAssignments().stream()
                .map(a -> a.getSecuredEndpointId()
                                + ":" +
                                (a.getScope() != null
                                        ? a.getScope().name().toUpperCase()
                                        : "")
                )
                .collect(Collectors.toSet());

        var incomingMap = request.getAssignments().stream()
                .collect(Collectors.toMap(
                        a -> a.getSecuredEndpointId()
                                + ":" +
                                (a.getScope() != null
                                        ? a.getScope().name().toUpperCase()
                                        : ""),
                        a -> a,
                        (a, b) -> a
                ));

        // -------------------------------------------------
        // DELETE REMOVED
        // -------------------------------------------------

        var toDelete = new HashSet<>(existingMap.keySet());
        toDelete.removeAll(incomingKeys);

        for (String key : toDelete) {

            var entity = existingMap.get(key);

            if (entity != null) {
                roleEndpointRepository.delete(entity.getId());
            }
        }

        // -------------------------------------------------
        // UPSERT
        // -------------------------------------------------

        for (String key : incomingKeys) {

            var assignment = incomingMap.get(key);
            var existingEntry = existingMap.get(key);

            var entity = existingEntry != null
                    ? existingEntry
                    : new RoleEndpoint();

            entity.setRoleId(roleId);
            entity.setRoleName(role.name);

            entity.setSecuredEndpointId(
                    assignment.getSecuredEndpointId()
            );

            entity.setScope(
                    assignment.getScope() != null
                            ? assignment.getScope().name().toUpperCase()
                            : null
            );

            if (existingEntry == null) {
                roleEndpointRepository.create(entity);
            } else {
                roleEndpointRepository.update(entity);
            }
        }
    }

}
