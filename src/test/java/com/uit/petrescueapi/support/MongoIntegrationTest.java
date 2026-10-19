package com.uit.petrescueapi.support;

import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;

/**
 * Base class for MongoDB adapter tests: one shared single-node replica set
 * (Testcontainers starts {@code mongo} with {@code --replSet}), so multi-document
 * transactions work exactly like in dev/prod.
 *
 * <p>Singleton container: started once per JVM and never stopped per class. With the
 * {@code @Testcontainers/@Container} lifecycle the container is stopped after each class while
 * Spring keeps the cached context pointing at the dead port.</p>
 */
@DataMongoTest(properties = "spring.data.mongodb.uuid-representation=standard")
public abstract class MongoIntegrationTest {

    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    static {
        MONGO.start();
    }
}
