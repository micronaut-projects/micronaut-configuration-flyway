package io.micronaut.flyway

import io.micronaut.flyway.event.MigrationFinishedEvent
import io.micronaut.flyway.event.SchemaCleanedEvent
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.runtime.event.annotation.EventListener
import jakarta.inject.Singleton
import org.flywaydb.core.Flyway
import spock.lang.Specification
import spock.util.concurrent.PollingConditions
import javax.sql.DataSource

class EventListenerSpec extends Specification {

    void 'test SchemaCleaned and MigrationFinished events are fired when Flyway cleans and migrate the schema'() {
        given: 'a configuration to clean the schema and run the migrations'
        ApplicationContext applicationContext = ApplicationContext.run(
            [
                'spec.name'                                    : EventListenerSpec.simpleName,
                'datasources.default.url'                      : 'jdbc:h2:mem:flywayEvent;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username'                 : 'sa',
                'datasources.default.password'                 : '',
                'datasources.default.driverClassName'          : 'org.h2.Driver',

                'jpa.default.packages-to-scan'                 : ['example.micronaut'],
                'jpa.default.properties.hibernate.hbm2ddl.auto': 'none',
                'jpa.default.properties.hibernate.show_sql'    : true,

                'flyway.datasources.default.locations'         : 'classpath:moremigrations',
                'flyway.datasources.default.clean-schema'      : true,
            ] as Map,
            Environment.TEST
        )

        when: 'running the migrations'
        applicationContext.getBean(DataSource)
        applicationContext.getBean(FlywayConfigurationProperties)
        applicationContext.getBean(Flyway)

        then: 'the events are fired'
        new PollingConditions().eventually {
            applicationContext.getBean(TestEventListener).migrationFinishedEvents.size() == 1
            applicationContext.getBean(TestEventListener).schemaCleanedEvents.size() == 1
        }
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'EventListenerSpec')
    static class TestEventListener {
        List<MigrationFinishedEvent> migrationFinishedEvents = []
        List<SchemaCleanedEvent> schemaCleanedEvents = []

        @EventListener
        void onStartupMigrate(MigrationFinishedEvent event) {
            migrationFinishedEvents.add(event)
        }

        @EventListener
        void onStartupCleaned(SchemaCleanedEvent event) {
            schemaCleanedEvents.add(event)
        }
    }
}
