package com.uit.petrescueapi.infrastructure.config;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Registers per-feature transaction-manager aliases so domain services can declare
 * {@code @Transactional(CHAT_TX)} once and get JPA or MongoDB depending on {@code app.storage.*}.
 */
public class StorageTransactionAliases implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    public static final String CHAT_TX = "chatTransactionManager";
    public static final String POST_TX = "postTransactionManager";

    private static final String JPA_TX = "transactionManager";

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        registry.registerAlias(target("app.storage.chat"), CHAT_TX);
        registry.registerAlias(target("app.storage.post"), POST_TX);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    }

    private String target(String property) {
        return "mongo".equalsIgnoreCase(environment.getProperty(property, "postgres")) ? MongoConfig.MONGO_TX : JPA_TX;
    }
}
