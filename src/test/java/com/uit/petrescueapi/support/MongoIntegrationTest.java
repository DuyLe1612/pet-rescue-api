package com.uit.petrescueapi.support;

import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for MongoDB adapter tests: one shared single-node replica set
 * (Testcontainers starts {@code mongo} with {@code --replSet}), so multi-document
 * transactions work exactly like in dev/prod.
 */
@Testcontainers
@DataMongoTest(properties = "spring.data.mongodb.uuid-representation=standard")
public abstract class MongoIntegrationTest {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");
}
