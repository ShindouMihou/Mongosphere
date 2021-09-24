package pw.mihou.mongosphere.core;

import com.mongodb.ConnectionString;
import com.mongodb.Function;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import pw.mihou.mongosphere.Mongosphere;
import pw.mihou.mongosphere.annotations.MongoConstructor;
import pw.mihou.mongosphere.annotations.MongoIgnore;
import pw.mihou.mongosphere.annotations.MongoItem;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MongoBase {

    private final MongoClient client;

    /**
     * Creates a new MongoBase with customized settings.
     *
     * @param settings The settings to use.
     */
    public MongoBase(MongoClientSettings settings) {
        this.client = MongoClients.create(settings);
    }

    /**
     * Creates a new MongoBase with default {@link pw.mihou.mongosphere.Mongosphere} settings.
     *
     * @param uri The URI to the MongoDB instance.
     */
    public MongoBase(ConnectionString uri) {
        this(MongoClientSettings.builder()
                .applyConnectionString(uri)
                .retryWrites(true)
                .retryReads(true)
                .build());
    }

    public MongoData fromDatabase(String database) {
        return new MongoData(client.getDatabase(database));
    }

    public static class MongoData {

        private final MongoDatabase database;

        /**
         * Creates a new MongoData instance that abstracts around the
         * {@link MongoDatabase} for extra methods.
         *
         * @param database The database to use.
         */
        public MongoData(MongoDatabase database) {
            this.database = database;
        }

        /**
         * Retrieves or creates a Collection if it exists or not, you can then use the
         * instance to retrieve or manage documents.
         *
         * @param collection The collection to find or create.
         * @return A new {@link MongoCollective}.
         */
        public MongoCollective fromCollection(String collection) {
            return new MongoCollective(database.getCollection(collection));
        }

        /**
         * Retrieves the name of the database, for some reason.
         *
         * @return The name of the database to fetch.
         */
        public String getName() {
            return database.getName();
        }

        /**
         * Drops this database from existence.
         */
        public void drop() {
            database.drop();
        }

        /**
         * Retrieves the original database that is usually used if you want to use
         * another function that isn't used here.
         *
         * @return The raw {@link MongoDatabase} object.
         */
        public MongoDatabase asRaw() {
            return database;
        }

        public static class MongoCollective {

            private final MongoCollection<Document> collection;

            /**
             * Creates a new MongoCollective instance that wraps around the {@link MongoCollection}
             * to add new methods.
             *
             * @param collection The collection to use.
             */
            public MongoCollective(MongoCollection<Document> collection) {
                this.collection = collection;
            }

            /**
             * Retrieves a list of all the documents that matches the specified filters
             * and transforms them to their respective classes.
             *
             * @param clazz The class to transform.
             * @param filters The filters to use.
             * @param <T> The type to transform into.
             * @return A list of the specified type.
             */
            public <T> List<T> find(Class<T> clazz, Bson... filters) {
                List<T> list = new ArrayList<>();
                collection.find(Filters.and(filters)).forEach(document -> list.add(fromDocument(clazz, document)));

                return list;
            }

            /**
             * Retrieves the first document that matches the specified filters
             * and transforms it into the specified class.
             *
             * @param clazz The class to transform.
             * @param filters The filters to use.
             * @param <T> the type to transform into.
             * @return A list of the specified type.
             */
            public <T> T findFirst(Class<T> clazz, Bson... filters) {
                return fromDocument(clazz, collection.find(Filters.and(filters)).first());
            }

            /**
             * Retrieves the first document that matches the key-value on the document.
             *
             * @param clazz The class to transform.
             * @param key The key to search for.
             * @param value The value the key must have.
             * @param <T> The type to transform into.
             * @return A list of the specified type.
             */
            public <T> T findWhere(Class<T> clazz, String key, Object value) {
                return findFirst(clazz, Filters.eq(key, value));
            }

            /**
             * Retrieves all the documents that matches the key-value on the document.
             *
             * @param clazz The class to transform.
             * @param key The key to search for.
             * @param value The value the key must have.
             * @param <T> The type to transform into.
             * @return A list of the specified type.
             */
            public <T> List<T> findAllWhere(Class<T> clazz, String key, Object value) {
                return find(clazz, Filters.eq(key, value));
            }

            /**
             * Performs a customized find where the only job of {@link Mongosphere} would be to cast
             * the {@link Document} to the specified class.
             *
             * @param clazz The class to transform into.
             * @param method The method to fetch the document.
             * @param <T> The type to transform into.
             * @return A new instance of the class.
             */
            public <T> T find(Class<T> clazz, Function<MongoCollection<Document>, Document> method) {
                return fromDocument(clazz, method.apply(collection));
            }

            /**
             * Inserts or updates the object stored in the database, if the identifier in the object exists
             * on the database then it will update that document else inserts it.
             *
             * @param object The object to insert.
             * @param identifier The identifier to find.
             */
            public void insertOrReplace(Object object, String identifier) {
                Document document = fromObject(object);
                collection.replaceOne(Filters.eq(identifier, document.get(identifier)), document, new ReplaceOptions().upsert(true));
            }

            /**
             * Inserts many objects to the database, this can create duplicates. If you don't want duplicates
             * then please use {@link MongoCollective#insertOrReplaceMany(List, String)} which will match
             * all of them.
             *
             * @param objects The objects to insert.
             */
            public void insertMany(List<?> objects) {
                collection.insertMany(objects.stream().map(o -> fromObject(objects)).collect(Collectors.toList()));
            }

            /**
             * Inserts many objects to the database or updates them if they match the identifier filter.
             *
             * @param objects The objects to insert.
             * @param identifier The identifier filter.
             */
            public void insertOrReplaceMany(List<?> objects, String identifier) {
                // I actually was thinking of doing updateMany but that would cause an error
                // if there are no documents, so I decided to go with this. Please improve if possible.
                objects.forEach(o -> insertOrReplace(o, identifier));
            }

            /**
             * Updates a single field instead of an entire document that matches the specified.
             *
             * @param object The object to gather data from.
             * @param field The field to match.
             * @param identifier The identifier to match.
             */
            public void updateField(Object object, String field, String identifier) {
                Document document = fromObject(object);

                collection.updateOne(Filters.eq(identifier, document.get(identifier)), Updates.set(field, document.get(field)));
            }

            /**
             * Updates a single field instead of an entire document that matches the specified.
             *
             * @param object The objects to gather data from.
             * @param fields The field to match.
             * @param identifier The identifier to match.
             */
            public void updateFields(Object object, List<String> fields, String identifier) {
                Document document = fromObject(object);

                collection.updateOne(Filters.eq(identifier, document.get(identifier)), Updates
                        .combine(fields.stream().map(s -> Updates.set(s, document.get(s))).toArray(Bson[]::new)));
            }

            /**
             * Inserts an object to the database.
             *
             * @param object The object to insert.
             */
            public void insert(Object object) {
                collection.insertOne(fromObject(object));
            }

            /**
             * Deletes many documents that matches the filter.
             *
             * @param filters The filters to match.
             */
            public void deleteMany(Bson... filters) {
                collection.deleteMany(Filters.and(filters));
            }

            /**
             * Deletes many documents that matches the key-value.
             *
             * @param identifier The identifier to match.
             * @param value The value required for a document to qualify in deletion.
             */
            public void deleteMany(String identifier, Object value) {
                collection.deleteMany(Filters.eq(identifier, value));
            }

            /**
             * Deletes one document that matches the filters.
             *
             * @param filters The filters to match.
             */
            public void deleteOne(Bson... filters) {
                collection.deleteOne(Filters.and(filters));
            }

            /**
             * Deletes one document that matches the key-value.
             *
             * @param identifier The identifier to match.
             * @param value The value required for a document to qualify in deletion.
             */
            public void deleteOne(String identifier, Object value) {
                collection.deleteOne(Filters.eq(identifier, value));
            }

            /**
             * Finds if a document matching the following arguments exists.
             *
             * @param identifier The identifier to search for.
             * @param value The value to match against.
             * @return Does a document matching exists?
             */
            public boolean has(String identifier, Object value) {
                return collection.find(Filters.eq(identifier, value)).first() != null;
            }

            /**
             * Retrieves the raw collection.
             *
             * @return The raw collection which you can use yourself.
             */
            public MongoCollection<Document> asRaw() {
                return collection;
            }

            private Document fromObject(Object object) {
                Document document = new Document();

                Arrays.stream(object.getClass().getDeclaredFields())
                        .forEach(field -> {
                            field.setAccessible(true);

                            try {
                                if (!field.isAnnotationPresent(MongoIgnore.class)) {
                                    String key = field.isAnnotationPresent(MongoItem.class) ?
                                            field.getAnnotation(MongoItem.class).key() : field.getName();

                                    document.put(key, field.get(object));
                                }
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        });

                return document;
            }

            private <T> T fromDocument(Class<T> clazz, Document document) {
                try {

                    T t;

                    if (Arrays.stream(clazz.getDeclaredConstructors()).anyMatch(constructor -> constructor.getParameterCount() == 0)) {
                        t = clazz.newInstance();

                        // We will only perform the filling of fields IF there are no constructors.
                        // This way, we won't interfere with what the constructors are doing.
                        Arrays.stream(t.getClass().getDeclaredFields())
                                .forEach(field -> {
                                    field.setAccessible(true);

                                    if (!field.isAnnotationPresent(MongoIgnore.class)) {
                                        String key = field.isAnnotationPresent(MongoItem.class) ?
                                                field.getAnnotation(MongoItem.class).key() : field.getName();

                                        try {
                                            Class<?> a = field.getType();

                                            // We want to check for a type adapter first.
                                            if (Mongosphere.hasAdapter(clazz)) {
                                                field.set(t, Mongosphere.getAdapter(clazz).from(document));
                                            } else {
                                                try {
                                                    if (a.equals(Boolean.class) || a.equals(boolean.class))
                                                        field.setBoolean(t, document.getBoolean(key));
                                                    else if (a.equals(Integer.class) || a.equals(int.class))
                                                        field.setInt(t, document.getInteger(key));
                                                    else if(a.equals(Long.class) || a.equals(long.class))
                                                        field.setLong(t, document.getLong(key));
                                                    else if(a.equals(Character.class) || a.equals(char.class))
                                                        field.setChar(t, document.get(key, Character.class));
                                                    else if(a.equals(String.class))
                                                        field.set(t, document.getString(key));
                                                    else if(a.equals(Double.class) || a.equals(double.class))
                                                        field.set(t, document.getDouble(key));
                                                    else
                                                        field.set(t, document.get(key, a));
                                                } catch (ClassCastException e) {
                                                    e.printStackTrace();
                                                    throw new IllegalStateException("Variable with key " + key +
                                                            " cannot be transformed, please register a TypeAdapter.");
                                                }
                                            }

                                        } catch (IllegalAccessException exception) {
                                            exception.printStackTrace();
                                        }
                                    }
                                });
                    } else {
                        // This is a masterpiece.
                        // A custom constructor must be signalled with MongoConstructor otherwise
                        // we'll check if any constructors matches the keys that the document has
                        // if there aren't then we'll just end up failing.
                        Constructor<T> constructor = (Constructor<T>) Arrays.stream(clazz.getDeclaredConstructors())
                                .filter(cons -> cons.isAnnotationPresent(MongoConstructor.class))
                                .findFirst().orElse(Arrays.stream(clazz.getDeclaredConstructors())
                                        .filter(cons -> Arrays.stream(cons.getParameters())
                                                .allMatch(parameter -> {
                                                    String key;
                                                    if (parameter.isNamePresent()) {
                                                        key = parameter.isAnnotationPresent(MongoItem.class) ?
                                                                parameter.getAnnotation(MongoItem.class).key() : parameter.getName();
                                                    } else {
                                                        if(parameter.isAnnotationPresent(MongoItem.class)) {
                                                            key = parameter.getAnnotation(MongoItem.class).key();
                                                        } else {
                                                            throw new IllegalStateException("A constructor parameter must be annotated with MongoItem for Mongosphere to use!");
                                                        }
                                                    }

                                                    return document.containsKey(key);
                                                }))
                                        .findFirst().orElseThrow(() -> new IllegalStateException("No constructor for " +  clazz.getName() +
                                                " is useable by Mongosphere!")));

                        constructor.setAccessible(true);
                        t = (T) constructor.newInstance(Arrays.stream(constructor.getParameters())
                                .map(parameter -> {
                                    String key;
                                    if (parameter.isNamePresent()) {
                                        key = parameter.isAnnotationPresent(MongoItem.class) ?
                                                parameter.getAnnotation(MongoItem.class).key() : parameter.getName();
                                    } else {
                                        if(parameter.isAnnotationPresent(MongoItem.class)) {
                                            key = parameter.getAnnotation(MongoItem.class).key();
                                        } else {
                                            throw new IllegalStateException("A constructor parameter must be annotated with MongoItem for Mongosphere to use!");
                                        }
                                    }

                                    Class<?> a = parameter.getType();

                                    // This is overpowered.
                                    try {
                                        if (a.equals(Boolean.class) || a.equals(boolean.class))
                                            return document.getBoolean(key);
                                        else if (a.equals(Integer.class) || a.equals(int.class))
                                            return document.getInteger(key);
                                        else if(a.equals(Long.class) || a.equals(long.class))
                                            return document.getLong(key);
                                        else if(a.equals(Character.class) || a.equals(char.class))
                                            return document.get(key, Character.class);
                                        else if(a.equals(String.class))
                                            return document.getString(key);
                                        else if(a.equals(Double.class) || a.equals(double.class))
                                            return document.getDouble(key);
                                        else
                                            return document.get(key, a);

                                    } catch (ClassCastException e) {
                                        e.printStackTrace();
                                        throw new IllegalStateException("Construction of " + clazz.getName() +" failed: Paramter with key " + key +
                                                " cannot be transformed, please register a TypeAdapter.");
                                    }
                                }).toArray());
                    }
                    return t;
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    return null;
                }
            }


        }

    }

}