package org.grnet.endpoint.scanner.runtime;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.ForbiddenException;
import org.grnet.endpoint.scanner.runtime.entitlements.OIDCEntitlementService;

@Interceptor
@SecuredEndpoint(resource = "", action = "", description = "")
@Priority(10)
public class SecuredEndpointInterceptor {

    @Inject
    OIDCEntitlementService oidcEntitlementService;

    @AroundInvoke
    Object checkAccess(InvocationContext context) throws Exception {

        var entitlements = oidcEntitlementService.fetchEntitlements();

        if(oidcEntitlementService.isSuperAdmin(entitlements)){
            return context.proceed();
        } else {
            throw new ForbiddenException("You cannot access this resource!");
        }
    }
}
