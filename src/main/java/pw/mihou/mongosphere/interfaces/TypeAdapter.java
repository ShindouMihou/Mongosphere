package pw.mihou.mongosphere.interfaces;

import org.bson.Document;

public interface TypeAdapter<T> {

    /**
     * Translates a {@link Document} into the specified class.
     *
     * @param document The document to retrieve data from to translate.
     * @return The translated object.
     */
    T from(Document document);

}
