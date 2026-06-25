package org.grnet.endpoint.scanner.deployment;

import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import org.grnet.endpoint.scanner.runtime.SecuredEndpoint;

import java.util.List;
import java.util.Set;

public class AuthorizationAnnotationsRegistrar implements InterceptorBindingRegistrar {

    static final List<InterceptorBinding> SECURITY_BINDINGS = List.of(
            InterceptorBinding.of(SecuredEndpoint.class, Set.of("params","scope"))
    );

    @Override
    public List<InterceptorBinding> getAdditionalBindings() {
        return SECURITY_BINDINGS;
    }
//static final List<InterceptorBinding> SECURITY_BINDINGS =
//        List.of(InterceptorBinding.of(SecuredEndpoint.class));
//
//    @Override
//    public List<InterceptorBinding> getAdditionalBindings() {
//        return SECURITY_BINDINGS;
//    }
}