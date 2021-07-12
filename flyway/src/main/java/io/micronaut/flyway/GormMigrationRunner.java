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
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.inject.Singleton;
import org.grails.orm.hibernate.HibernateDatastore;
import org.grails.orm.hibernate.connections.HibernateConnectionSource;

import javax.sql.DataSource;

/**
 * Run migrations when using GORM.
 *
 * @author Iván López
 * @since 1.1.0
 */
@Singleton
@Requires(classes = {HibernateDatastore.class})
@Requires(property = "data-source")
public class GormMigrationRunner extends AbstractFlywayMigration implements BeanCreatedEventListener<HibernateDatastore> {

    /**
     * @param applicationContext The application context
     * @param eventPublisher     The event publisher
     */
    GormMigrationRunner(ApplicationContext applicationContext,
                        ApplicationEventPublisher eventPublisher) {
        super(applicationContext, eventPublisher);
    }

    @Override
    public HibernateDatastore onCreated(BeanCreatedEvent<HibernateDatastore> event) {
        HibernateDatastore hibernateDatastore = event.getBean();

        hibernateDatastore.getConnectionSources().forEach(connectionSource -> {
            String qualifier = connectionSource.getName();
            DataSource dataSource = ((HibernateConnectionSource) connectionSource).getDataSource();

            applicationContext
                    .findBean(FlywayConfigurationProperties.class, Qualifiers.byName(qualifier))
                    .ifPresent(flywayConfig -> run(flywayConfig, dataSource));
        });

        return hibernateDatastore;
    }
}
