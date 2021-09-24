package pw.mihou.mongosphere.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface MongoItem {

    /**
     * This is used to indicate that this variable
     * has a different key from the variable name.
     *
     * @return The key name of the variable.
     */
    String key() default "";

}
