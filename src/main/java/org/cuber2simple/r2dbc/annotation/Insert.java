package org.cuber2simple.r2dbc.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Insert {
    /**
     * Returns an SQL for inserting record(s).
     *
     * @return an SQL for inserting record(s)
     */
    String value();
}
