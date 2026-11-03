package com.uit.petrescueapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;

@SpringBootTest
class PetRescueApiApplicationTests {

    // MongoSchemaInitializer creates collections on startup, so the context needs a real replica set.
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    static {
        MONGO.start();
    }

    @Test
    void contextLoads() {
    }

}
