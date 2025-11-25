package org.grnet.auth.interceptors;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.ForbiddenException;
import org.grnet.auth.entitlements.AccessControlService;

import java.util.ArrayList;
import java.util.Arrays;

@CheckEntitlements
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class CheckEntitlementsInterceptor {

    @Inject
    AccessControlService accessControlService;

    @AroundInvoke
    public Object check(InvocationContext context) throws Exception {

        CheckEntitlements ann =
                context.getMethod().getAnnotation(CheckEntitlements.class) != null
                        ? context.getMethod().getAnnotation(CheckEntitlements.class)
                        : context.getTarget().getClass().getAnnotation(CheckEntitlements.class);

        // If requireSuperAdmin is set, check global super_admin and bypass everything else
        if (ann.requireSuperAdmin()) {
            if (!accessControlService.isSuperAdmin()) {
                throw new ForbiddenException("Access denied — super admin privileges required.");
            }
            return context.proceed();
        }

        // Check specific group/role/hierarchy access
        var group = ann.group();
        var role = ann.role();
        var hierarchy = new ArrayList<>(Arrays.asList(ann.hierarchy()));

        boolean allowed = accessControlService.hasAccess(group, role, hierarchy);

        if (!allowed) {
            throw new ForbiddenException(String.format("Access denied — required group='%s', role='%s'", group, role)
            );
        }
        return context.proceed();
    }
}
