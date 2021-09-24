package pw.mihou.mongosphere.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tells {@link pw.mihou.mongosphere.Mongosphere} to ignore this
 * variable from being filled up.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MongoIgnore {
}
