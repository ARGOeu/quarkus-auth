package org.grnet.endpoint.scanner.runtime.validators.constraints;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.grnet.endpoint.scanner.runtime.validators.RoleValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@RegisterForReflection
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RoleValidator.class)
public @interface ValidRole {
    String message() default "Invalid role";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
