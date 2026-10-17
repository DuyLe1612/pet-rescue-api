package com.uit.petrescueapi.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

class MongoConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    TransactionAutoConfiguration.class,
                    MongoAutoConfiguration.class,
                    MongoDataAutoConfiguration.class))
            .withUserConfiguration(MongoConfig.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:txtest;DB_CLOSE_DELAY=-1",
                    "spring.jpa.hibernate.ddl-auto=none");

    @Test
    void jpaTransactionManagerStaysPrimaryWhenMongoManagerIsAdded() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            // plain @Transactional must keep resolving to JPA
            assertThat(ctx.getBean(PlatformTransactionManager.class)).isInstanceOf(JpaTransactionManager.class);
            assertThat(ctx.getBean("transactionManager")).isInstanceOf(JpaTransactionManager.class);
        });
    }

    @Test
    void exposesNamedMongoTransactionManager() {
        runner.run(ctx -> assertThat(ctx.getBean(MongoConfig.MONGO_TX, MongoTransactionManager.class)).isNotNull());
    }
}
