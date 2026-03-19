package org.grnet.endpoint.scanner.runtime;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.ForbiddenException;
import org.grnet.endpoint.scanner.runtime.entitlements.EntitlementProvider;

@Interceptor
@SecuredEndpoint
@Priority(10)
public class SecuredEndpointInterceptor {

    @Inject
    EntitlementProvider entitlementProvider;

    @AroundInvoke
    Object checkAccess(InvocationContext context) throws Exception {

        var entitlements = entitlementProvider.fetchEntitlements();

        if(true){
            return context.proceed();
        } else {
            throw new ForbiddenException("You cannot access this resource!");
        }
    }
}
