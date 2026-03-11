package org.grnet.endpoint.scanner.runtime.resolvers;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface TestResolver {
    Class<? extends TestGroupIdResolver> idResolver() default NoOpTestResolver.class;
    String pathId() default "";
}

