package org.grnet.auth.entitlements;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@ApplicationScoped
public class AccessControlService {

    @ConfigProperty(name = "quarkus.auth.entitlements.parent.group")
    String parentGroup;

    @Inject
    OIDCEntitlementService oidc;

    public boolean hasAccess(String group, String role, List<String> targetHierarchy) {

        var effectiveGroup = (group == null || group.isBlank())
                ? parentGroup
                : group;

        var entitlements = oidc.fetchEntitlements();

        if (targetHierarchy == null || targetHierarchy.isEmpty()) {
            return entitlements.stream()
                    .anyMatch(e -> e.getGroup().equals(effectiveGroup)
                            && e.getRole().equals(role)
                            && (isSuperAdmin() || !e.hasHierarchy()));
        }

        return entitlements.stream()
                .filter(e -> e.getGroup().equals(effectiveGroup) && e.getRole().equals(role))
                .anyMatch(e -> isSuperAdmin() || hierarchyCovers(e.getHierarchy(), targetHierarchy));
    }

    private boolean hierarchyCovers(List<String> entitlementHierarchy, List<String> targetHierarchy) {
        if (entitlementHierarchy.size() > targetHierarchy.size()) return false;
        for (int i = 0; i < entitlementHierarchy.size(); i++)
            if (!entitlementHierarchy.get(i).equals(targetHierarchy.get(i))) return false;
        return true;
    }

    public boolean isSuperAdmin() {
        return oidc.fetchEntitlements().stream()
                .anyMatch(e -> "super_admin".equals(e.getRole()) && !e.hasHierarchy());
    }
}
