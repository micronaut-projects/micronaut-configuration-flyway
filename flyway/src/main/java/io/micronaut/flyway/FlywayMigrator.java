/*
 * Copyright 2017-2021 original authors
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
import jakarta.inject.Singleton;

import javax.sql.DataSource;

/**
 * Public access to invoke Flyway migrations when DataSource onCreate behaviour is not desired.
 * <p>
 * The Flyway configuration should be:
 *     flyway.enabled = true
 *     flyway.datasources.*.enabled = false
 * <p>
 * This ensures that Flyway won't run automatically the migrations. The following service can then be injected later
 * and execute {@code run} to execute the migrations based on a given {@link FlywayConfigurationProperties}.
 *
 * @author Iván López
 * @since 3.6.0
 */
@Singleton
public class FlywayMigrator extends AbstractFlywayMigration {

    /**
     * @param applicationContext The application context
     * @param eventPublisher     The event publisher
     */
    FlywayMigrator(ApplicationContext applicationContext,
                   ApplicationEventPublisher eventPublisher) {
        super(applicationContext, eventPublisher);
    }

    /**
     * Run the Flyway migrations for a specific config and a DataSource.
     *
     * @param config     The {@link FlywayConfigurationProperties}
     * @param dataSource The {@link DataSource}
     */
    public void run(FlywayConfigurationProperties config,
                              DataSource dataSource) {
        super.forceRun(config, dataSource);
    }
}
