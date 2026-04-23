package org.grnet.endpoint.scanner.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SecuredEndpoint {
    //Class<? extends ApiResource> resource() default NoResource.class;

    ParamRef[] pathParamRef() default {};

    ParamRef[] bodyParamRef() default {};
}