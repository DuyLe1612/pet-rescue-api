package com.uit.petrescueapi.infrastructure.config;

import com.uit.petrescueapi.infrastructure.persistence.mongo.MongoSchemaInitializer;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Two transaction managers live side by side (see migrate.md §7.1):
 * <ul>
 *   <li>{@code transactionManager} — JPA/PostgreSQL, {@code @Primary}, used by plain {@code @Transactional}.</li>
 *   <li>{@code mongoTransactionManager} — MongoDB (chat, posts, comments, likes), used by their
 *       domain services via {@code @Transactional(transactionManager = "mongoTransactionManager")}.</li>
 * </ul>
 * The JPA manager is declared explicitly because Spring Boot's JPA auto-configuration backs off
 * as soon as <i>any</i> {@code TransactionManager} bean exists.
 */
@Configuration
public class MongoConfig {

    public static final String MONGO_TX = "mongoTransactionManager";

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(MONGO_TX)
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }

    @Bean
    public MongoSchemaInitializer mongoSchemaInitializer(MongoTemplate mongoTemplate) {
        return new MongoSchemaInitializer(mongoTemplate);
    }
}
