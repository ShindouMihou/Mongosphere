## 🌎 Mongosphere
Mongosphere is a Java library wrapped around MongoDB Driver Sync with the aim of speeding up development overall and also 
reduce overall lines of code written with the assistance of Reflection.

## 🎂 Installation
You can easily install this library from Central Maven:

### Maven
```xml
<dependency>
  <groupId>pw.mihou</groupId>
  <artifactId>Mongosphere</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Gradle
```groovy
implementation 'pw.mihou:Mongosphere:1.0.0'
```

## 🤔 Wiki?
Please read our wiki for a more detailed explanation of many of the things here from creating the models to inserting a model and fetching a model from the database, here is a little table of contents in proper order of what you should read first.
- [Annotations](https://github.com/ShindouMihou/Mongosphere/wiki/Annotations)
- [Creating Models](https://github.com/ShindouMihou/Mongosphere/wiki/Creating-Models)
- [Creating Mongosphere](https://github.com/ShindouMihou/Mongosphere/wiki/Creating-Mongosphere)
- [Inserting Models with Mongosphere](https://github.com/ShindouMihou/Mongosphere/wiki/Inserting-and-Updating-Models-with-Mongosphere)
- [Fetching Models with Mongosphere](https://github.com/ShindouMihou/Mongosphere/wiki/Fetching-Models-with-Mongosphere)

## 💬 Usage
A very simple example of Mongosphere usage is can be seen below, please read our wiki for a more detailed
introduction over Mongosphere like what to do if Mongosphere cannot detect a constructor or cannot parse a type, etc.

### TestModel.class

```java
import pw.mihou.mongosphere.annotations.MongoConstructor;
import pw.mihou.mongosphere.annotations.MongoItem;

public class TestModel {

    private String id;
    @MongoItem(key = "content")
    private String someContent;

    @MongoConstructor
    public TestModel(@MongoItem(key = "id") String id, @MongoItem(key = "content") String someContent) {
        this.id = id;
        this.someContent = someContent;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return someContent;
    }
}
```

You may notice that we are using several annotations and here is the explanation for each of those annotations:
- **@MongoItem**: This is an annotation that helps the library identify which value to fill with, it can be used as
an annotation for variables and parameters. If used in a parameter, it tells the library to fill the parameter with the correct value from the database,
if used in a variable, it tells the library to fill that variable with the annotation.
- **@MongoConstructor**: This tells the library to use this constructor for constructing the object.

A few things to note, if there is a constructor that Mongosphere can use then it will only leave the filling of the variables to the constructor, which 
means instead of setting the variables, it will instead fill the constructor's parameters and leave the assignment to the constructor.

```
No constructor:
variableA, variableB, variableC will be filled up by Mongosphere.

Constructor that has variableA as a parameter:
Mongosphere will hand the value of variableA to the constructor's parameter and no longer move from there, to avoid 
interferring with whatever the constructor is doing (in other words, prevent variable overriding).
```

After the model is set, you can easily do the following:
```java
import com.mongodb.ConnectionString;
import pw.mihou.mongosphere.Mongosphere;
import pw.mihou.mongosphere.core.MongoBase;

public class Test {

    private static MongoBase base;

    public static void main(String[] args) {
        // Initialize Mongosphere.
        base = Mongosphere.create(new ConnectionString(System.getenv("mongodb")));;
    }

    /**
     * This retrieves a model that has the id of hello on MongoDB and turn
     * the returned data into the TestModel.
     *
     * @return The test model returned.
     */
    public TestModel getExample() {
        return base.fromDatabase("databaseName")
                .fromCollection("collectionName")
                .findWhere(TestModel.class, "id", "ID"); // the second ID refers to the value below.
    }

    /**
     * This inserts a model into the database using Mongosphere.
     */
    public void insertExample() {
        TestModel model = new TestModel("ID", "CONTENT");

        // If you want to automatically replace an existing one with the newer model.
        base.fromDatabase("databaseName")
                .fromCollection("collectionName")
                .insertOrReplace(model, "id");

        // If you want to insert the model even if it means duplicating it.
        base.fromDatabase("databaseName")
                .fromCollection("collectionName")
                .insert(model);
    }

}
```
