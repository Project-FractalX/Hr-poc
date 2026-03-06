package org.fractalx.annotations;

import java.lang.annotation.*;

/**
 * FractalX stub annotation — declares which services are allowed to call this class.
 * Replace with the real fractalx-annotations dependency once available.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServiceBoundary {
    /** Service names that are allowed to call this class. */
    String[] allowedCallers() default {};
    /** Whether the verifier fails hard on violations. */
    boolean strict() default true;
}
