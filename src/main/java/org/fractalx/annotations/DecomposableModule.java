package org.fractalx.annotations;

import java.lang.annotation.*;

/**
 * FractalX stub annotation — marks the boundary class of a module.
 * Replace with the real fractalx-annotations dependency once available.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DecomposableModule {
    /** Unique kebab-case service name (e.g. "employee-service"). */
    String serviceName();
    /** Port for the generated microservice. 0 = auto-assign. */
    int port() default 0;
    /** Whether to generate an independent deployable service. */
    boolean independentDeployment() default true;
    /** Logical schema names owned by this module (informational). */
    String[] ownedSchemas() default {};
}
