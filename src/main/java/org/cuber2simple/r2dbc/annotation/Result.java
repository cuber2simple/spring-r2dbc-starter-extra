package org.cuber2simple.r2dbc.annotation;

import org.cuber2simple.r2dbc.convert.NoConverter;
import org.springframework.core.convert.converter.Converter;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Results.class)
public @interface Result {
    /**
     * Returns whether id column or not.
     *
     * @return {@code true} if id column; {@code false} if otherwise
     */
    boolean id() default false;

    /**
     * Return the column name(or column label) to map to this argument.
     *
     * @return the column name(or column label)
     */
    String column() default "";

    /**
     * Returns the property name for applying this mapping.
     *
     * @return the property name
     *
     */
    String property() default "";

    /**
     * Return the java type for this argument.
     *
     * @return the java type
     */
    Class<?> javaType() default void.class;


    /**
     * Returns the {@link Converter} type for retrieving a column value from result set.
     *
     * @return the {@link Converter} type
     */
    Class<? extends Converter> converter() default NoConverter.class;


    /**
     * Returns the ID of resultMap to map data to this property
     *
     * @return the resultMap ID
     */
    String resultMap() default "";
}
