package org.fractalx.annotations;

import java.lang.annotation.*;

/**
 * FractalX stub annotation — marks a saga entry-point method.
 * Replace with the real fractalx-annotations dependency once available.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedSaga {
    /** Unique kebab-case saga ID. */
    String sagaId();
    /** Name of the overall rollback method in the same class. */
    String compensationMethod();
    /** Saga timeout in milliseconds. */
    long timeout() default 30_000;
    /** Optional step hints shown in the Admin UI. */
    String[] steps() default {};
    /** Human-readable description shown in the Admin UI. */
    String description() default "";
}
