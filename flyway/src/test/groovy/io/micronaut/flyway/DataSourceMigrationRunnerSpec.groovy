package io.micronaut.flyway

import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires
import io.micronaut.jdbc.DataSourceResolver
import jakarta.inject.Singleton

import javax.sql.DataSource

class DataSourceMigrationRunnerSpec extends ApplicationContextSpecification {

    @Override
    String getSpecName() {
        'DataSourceMigrationRunnerSpec'
    }
    @Override
    Map<String, Object> getConfiguration() {
        super.configuration +
                getDataSourceConfiguration('datasourcemigrationrunner') +
                getJpaConfiguration(['example.micronaut'])
    }
    void "DataSourceMigrationRunner::onCreated returns wrapped Datasource"() {
        when:
        DataSource dataSource = applicationContext.getBean(DataSource)

        then:
        !(dataSource instanceof ReturnedByDataSourceResolver)
    }

    @Primary
    @Requires(property = 'spec.name', value = 'DataSourceMigrationRunnerSpec')
    @Singleton
    static class MockDataSourceResolver implements DataSourceResolver {
        @Override
        DataSource resolve(DataSource dataSource) {
            new ReturnedByDataSourceResolver(dataSource)
        }
    }

    static class ReturnedByDataSourceResolver implements DataSource {

        @Delegate
        private final DataSource dataSource

        ReturnedByDataSourceResolver(DataSource dataSource) {
            this.dataSource = dataSource
        }
    }
}
