package org.grnet.endpoint.scanner.runtime.process;

import jakarta.enterprise.util.AnnotationLiteral;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.PARAMETER,
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.TYPE
})
public @interface AfterProcessing {
    String endpoint();

    class Literal extends AnnotationLiteral<AfterProcessing> implements AfterProcessing {
        private final String endpoint;

        public Literal(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public String endpoint() {
            return endpoint;
        }
    }
}

