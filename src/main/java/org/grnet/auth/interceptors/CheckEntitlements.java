package org.grnet.auth.interceptors;

import jakarta.interceptor.InterceptorBinding;
import jakarta.enterprise.util.Nonbinding;
import java.lang.annotation.*;

@InterceptorBinding
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface CheckEntitlements {

    @Nonbinding
    boolean requireSuperAdmin() default false;

//    @Nonbinding
//    boolean requireAdmin() default false;

    @Nonbinding
    String group() default "";

    @Nonbinding
    String role() default "";

    @Nonbinding
    String[] hierarchy() default {};

    @Nonbinding
    String pathParam() default "";
}
