/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.flyway;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.flyway.event.MigrationFinishedEvent;
import io.micronaut.flyway.event.SchemaCleanedEvent;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Singleton;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Parent class that runs Flyway database migrations.
 *
 * @author James Kleeh
 * @author Iván López
 * @since 1.0.0
 */
@Singleton
class AbstractFlywayMigration {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlywayMigration.class);

    final ApplicationContext applicationContext;
    final ApplicationEventPublisher eventPublisher;

    /**
     * @param applicationContext The application context
     * @param eventPublisher     The event publisher
     */
    AbstractFlywayMigration(ApplicationContext applicationContext, ApplicationEventPublisher eventPublisher) {
        this.applicationContext = applicationContext;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Run the Flyway migrations for a specific config and a dataSource only if the config is enabled.
     *
     * @param config     The {@link FlywayConfigurationProperties}
     * @param dataSource The {@link DataSource}
     */
    void run(FlywayConfigurationProperties config, DataSource dataSource) {
        if (config.isEnabled()) {
            forceRun(config, dataSource);
        }
    }

    /**
     * Run the Flyway migrations whether they are enabled or not, for the specific datasource.
     *
     * @param config     The {@link FlywayConfigurationProperties}
     * @param dataSource The {@link DataSource}
     */
    void forceRun(FlywayConfigurationProperties config, DataSource dataSource) {
        FluentConfiguration fluentConfiguration = config.getFluentConfiguration();
        fluentConfiguration.dataSource(dataSource);

        Flyway flyway = fluentConfiguration.load();
        this.applicationContext.registerSingleton(Flyway.class, flyway, Qualifiers.byName(config.getNameQualifier()), false);

        if (config.isAsync()) {
            runAsync(config, flyway);
        } else {
            runFlyway(config, flyway);
        }
    }

    private void runFlyway(FlywayConfigurationProperties config, Flyway flyway) {
        if (config.isCleanSchema()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Cleaning schema for database with qualifier [{}]", config.getNameQualifier());
            }
            flyway.clean();
            eventPublisher.publishEvent(new SchemaCleanedEvent(config));
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Running migrations for database with qualifier [{}]", config.getNameQualifier());
        }
        flyway.migrate();
        eventPublisher.publishEvent(new MigrationFinishedEvent(config));
    }

    /**
     * Run a migration asynchronously.
     *
     * @param config The {@link FlywayConfigurationProperties}
     * @param flyway The {@link Flyway} already configured to run the migrations
     */
    @Async(TaskExecutors.IO)
    void runAsync(FlywayConfigurationProperties config, Flyway flyway) {
        runFlyway(config, flyway);
    }
}
