package org.grnet.endpoint.scanner.runtime;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface ParamRef {

    String param() default "";
    ParamType type();

    Class<? extends ApiResource> referTo() default NoResource.class;
}

