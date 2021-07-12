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
import io.micronaut.context.condition.Condition;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.inject.Singleton;
import org.flywaydb.core.internal.jdbc.DriverDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Run migrations when the {@link DataSource} is only specified in Flyway configuration and there is no Micronaut
 * DataSource defined.
 *
 * @author James Kleeh
 * @author Iván López
 * @since 1.0.0
 */
@Singleton
public class AlternativeMigrationRunner extends AbstractFlywayMigration implements BeanCreatedEventListener<FlywayConfigurationProperties> {

    static final Logger LOG = LoggerFactory.getLogger(Condition.class);

    /**
     * @param applicationContext The application context
     * @param eventPublisher     The event publisher
     */
    public AlternativeMigrationRunner(ApplicationContext applicationContext,
                                      ApplicationEventPublisher eventPublisher) {
        super(applicationContext, eventPublisher);
    }

    @Override
    public FlywayConfigurationProperties onCreated(BeanCreatedEvent<FlywayConfigurationProperties> event) {
        FlywayConfigurationProperties config = event.getBean();
        String name = config.getNameQualifier();

        if (config.isEnabled()) {
            if (config.hasAlternativeDatabaseConfiguration()) {
                DataSource dataSource = new DriverDataSource(Thread.currentThread().getContextClassLoader(), null, config.getUrl(), config.getUser(), config.getPassword());
                run(config, dataSource);
            } else {
                if (!applicationContext.containsBean(DataSource.class, Qualifiers.byName(name))) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("* Flyway bean not created for identifier [" + name + "] because no data source was found with a named qualifier of the same name.");
                    }
                }
            }
        }

        return config;
    }
}
