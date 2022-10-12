package org.cuber2simple.r2dbc.annotation;

import org.springframework.data.annotation.QueryAnnotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@QueryAnnotation
@Documented
public @interface DynamicQuery {

    /**
     * The Dynamic SQL statement to execute when the annotated method gets invoked.
     */
    String value();

    /**
     *
     * The Dynamic SQL statement using which language script
     */
    String lang() default "groovy";
}
