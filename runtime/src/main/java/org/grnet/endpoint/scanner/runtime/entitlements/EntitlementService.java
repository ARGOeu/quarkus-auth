package org.grnet.endpoint.scanner.runtime.entitlements;

import org.grnet.endpoint.scanner.runtime.SecuredEndpointConfig;

import java.util.List;

public interface EntitlementService {

    List<Entitlement> fetchEntitlements();

    default boolean isSuperAdmin(List<Entitlement> entitlements, SecuredEndpointConfig config){

        return entitlements
                .stream()
                .anyMatch(entitlement -> entitlement.getGroup().equals(config.parentGroup()) && entitlement.getRole().equals(config.superAdminRole()));
    }

    default boolean hasAccess(String group, String role, List<String> targetHierarchy) {

        if (targetHierarchy == null || targetHierarchy.isEmpty()) {

            return fetchEntitlements().stream()
                    .anyMatch(e -> e.getGroup().equals(group)
                            && e.getRole().equals(role)
                            && isSysAdmin());
        } else {

            return fetchEntitlements().stream()
                    .filter(e -> e.getGroup().equals(group) && e.getRole().equals(role))
                    .anyMatch(e -> isSysAdmin() || hierarchyCovers(e.getHierarchy(), targetHierarchy));
        }
    }

    default boolean hierarchyCovers(List<String> entitlementHierarchy, List<String> targetHierarchy) {

        if (entitlementHierarchy.size() > targetHierarchy.size()) return false;
        for (int i = 0; i < entitlementHierarchy.size(); i++) {
            if (!entitlementHierarchy.get(i).equals(targetHierarchy.get(i))) return false;
        }

        return true;
    }

    default boolean isSysAdmin() {

        return fetchEntitlements().stream()
                .anyMatch(e -> "admin".equals(e.getRole()) && !e.hasHierarchy());
    }
}
