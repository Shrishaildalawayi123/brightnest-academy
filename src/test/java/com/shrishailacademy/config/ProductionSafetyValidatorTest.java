package com.shrishailacademy.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ProductionSafetyValidatorTest {
    @Configuration
    static class Startup {
        static final AtomicBoolean initialized = new AtomicBoolean();
        @Bean
        @DependsOn("productionSafetyValidator")
        Object databaseInitializer() {
            initialized.set(true);
            return new Object();
        }
    }

    @Test
    void unsafeProductionConfigurationFailsBeforeInitializerCreation() {
        Startup.initialized.set(false);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("prod");
            context.register(Startup.class, ProductionSafetyValidator.class);
            assertThrows(BeanCreationException.class, context::refresh);
            assertFalse(Startup.initialized.get());
        }
    }
}
