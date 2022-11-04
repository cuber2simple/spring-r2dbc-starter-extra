package org.cuber2simple.r2dbc.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PublisherSelect {

    /**
     * 使用那个selectId
     *
     * @return
     */
    String selectId();

    /**
     * 保留的列值
     * @return
     */
    String[] holderColumns();
}
