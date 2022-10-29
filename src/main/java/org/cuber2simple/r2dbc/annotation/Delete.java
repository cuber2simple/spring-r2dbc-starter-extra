package org.cuber2simple.r2dbc.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Delete {
    /**
     * Returns an SQL for deleting record(s).
     *
     * @return an SQL for deleting record(s)
     */
    String value();
}
