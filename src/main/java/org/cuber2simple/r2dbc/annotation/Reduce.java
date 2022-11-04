package org.cuber2simple.r2dbc.annotation;


import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Reduce {
    /**
     * 前缀
     * @return
     */
    String prefix() default "";
}
