package org.fractalx.annotations;

import java.lang.annotation.*;

/**
 * FractalX stub annotation — configures the generated Admin UI service.
 * Replace with the real fractalx-annotations dependency once available.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminEnabled {
    int port() default 9090;
    String username() default "admin";
    String password() default "admin123";
    boolean monitoring() default true;
    boolean logging() default true;
}
