package org.cuber2simple.r2dbc.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Select {
    /**
     * Returns an SQL for retrieving record(s).
     *
     * @return an SQL for retrieving record(s)
     */
    String value();

    /**
     * select id, default = "package.ClassName.MethodName"
     * @return
     */
    String id();
}
