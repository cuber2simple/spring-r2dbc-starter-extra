package org.cuber2simple.r2dbc.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Update {

    /**
     * Returns an SQL for updating record(s).
     *
     * @return an SQL for updating record(s)
     */
    String value();

}
