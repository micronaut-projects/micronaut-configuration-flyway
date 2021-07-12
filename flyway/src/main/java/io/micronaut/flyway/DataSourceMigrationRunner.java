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
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.naming.NameResolver;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.jdbc.DataSourceResolver;
import jakarta.inject.Singleton;

import javax.sql.DataSource;

/**
 * Run migrations when there is a {@link DataSource} defined for it.
 *
 * @author James Kleeh
 * @author Iván López
 * @since 1.0.0
 */
@Singleton
public class DataSourceMigrationRunner extends AbstractFlywayMigration implements BeanCreatedEventListener<DataSource> {

    private final DataSourceResolver dataSourceResolver;

    /**
     * @param applicationContext The application context
     * @param eventPublisher     The event publisher
     * @param dataSourceResolver The data source resolver
     */
    public DataSourceMigrationRunner(ApplicationContext applicationContext,
                                     ApplicationEventPublisher eventPublisher,
                                     @Nullable DataSourceResolver dataSourceResolver) {
        super(applicationContext, eventPublisher);
        this.dataSourceResolver = dataSourceResolver != null ? dataSourceResolver : DataSourceResolver.DEFAULT;
    }

    @Override
    public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
        DataSource dataSource = event.getBean();
        if (event.getBeanDefinition() instanceof NameResolver) {
            ((NameResolver) event.getBeanDefinition())
                    .resolveName()
                    .ifPresent(name -> {
                        applicationContext
                                .findBean(FlywayConfigurationProperties.class, Qualifiers.byName(name))
                                .ifPresent(flywayConfig -> {
                                    DataSource unwrappedDataSource = dataSourceResolver.resolve(dataSource);
                                    run(flywayConfig, unwrappedDataSource);
                                });
                    });
        }
        return dataSource;
    }
}
