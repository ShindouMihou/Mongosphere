package pw.mihou.mongosphere;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import pw.mihou.mongosphere.core.MongoBase;
import pw.mihou.mongosphere.interfaces.TypeAdapter;

import java.util.HashMap;
import java.util.Map;

public class Mongosphere {

    private static final Map<Class, TypeAdapter> adapters = new HashMap<>();

    /**
     * Adds a new adapter to the Mongosphere database.
     *
     * @param clazz The class type that should be transformed.
     * @param adapter The adapter to use.
     */
    public static void addAdapter(Class<?> clazz, TypeAdapter<?> adapter) {
        adapters.put(clazz, adapter);
    }

    /**
     * Retrieves the adapter for the class.
     *
     * @param clazz The class type to transform.
     * @param <T> The type to transform.
     * @return The type adapter to use.
     */
    public static <T> TypeAdapter<T> getAdapter(Class<T> clazz) {
        return adapters.get(clazz);
    }

    /**
     * Checks if the class specified has a TypeAdapter.
     *
     * @param clazz The class to search for.
     * @return Does this class have a type adapter?
     */
    public static boolean hasAdapter(Class<?> clazz) {
        return adapters.containsKey(clazz);
    }

    /**
     * Creates a new {@link MongoBase} which is the base of Mongoshpere.
     *
     * @param settings The settings to use.
     * @return A new MongoBase instance.
     */
    public static MongoBase create(MongoClientSettings settings) {
        return new MongoBase(settings);
    }

    /**
     * Creates a new {@link MongoBase} which is the base of Mongoshpere.
     *
     * @param uri The connection URI to use.
     * @return A new MongoBase instance.
     */
    public static MongoBase create(ConnectionString uri) {
        return new MongoBase(uri);
    }

}
