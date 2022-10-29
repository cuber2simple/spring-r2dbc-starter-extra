package org.cuber2simple.r2dbc.annotation;

import java.lang.annotation.*;

/**
 * The annotation that be grouping mapping definitions for property.
 *
 * <p>
 * <b>How to use:</b>
 *
 * <pre>
 * public interface UserMapper {
 *   &#064;Results({
 *     &#064;Result(property = "id", column = "id", id = true),
 *     &#064;Result(property = "name", column = "name"),
 *     &#064;Result(property = "email" column = "id", one = @One(select = "selectUserEmailById", fetchType = FetchType.LAZY)),
 *     &#064;Result(property = "telephoneNumbers" column = "id", many = @Many(select = "selectAllUserTelephoneNumberById", fetchType = FetchType.LAZY))
 *   })
 *   &#064;Select("SELECT id, name FROM users WHERE id = #{id}")
 *   User selectById(int id);
 * }
 * </pre>
 *
 * @author Clinton Begin
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Results {
    /**
     * Returns the id of this result map.
     *
     * @return the id of this result map
     */
    String id() default "";

    /**
     * Returns mapping definitions for property.
     *
     * @return mapping definitions
     */
    Result[] value() default {};
}