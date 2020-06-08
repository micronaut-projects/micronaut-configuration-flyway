package io.micronaut.flyway.docs

import groovy.sql.Sql
import io.micronaut.flyway.FlywayConfigurationProperties
import io.micronaut.flyway.GormMigrationRunner
import io.micronaut.flyway.YamlAsciidocTagCleaner
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import org.flywaydb.core.Flyway
import org.yaml.snakeyaml.Yaml
import spock.lang.Shared
import spock.lang.Specification

class GormSpec extends Specification implements YamlAsciidocTagCleaner {

    String gormConfig = '''\
spec.name: GormDocSpec
//tag::yamlconfig[]
dataSource: # <1>
  pooled: true
  jmxExport: true
  dbCreate: none # <2>
  url: 'jdbc:h2:mem:GORMDb'
  driverClassName: org.h2.Driver
  username: sa
  password: ''
        
flyway:
  datasources: # <3>
    default: # <4>
      enabled: true # <5>
'''//end::yamlconfig[]

    @Shared
    Map<String, Object> flywayMap = [
        'spec.name': 'GormDocSpec',
        dataSource : [
            pooled         : true,
            jmxExport      : true,
            dbCreate       : 'none',
            url            : 'jdbc:h2:mem:GORMDb',
            driverClassName: 'org.h2.Driver',
            username       : 'sa',
            password       : ''
        ],
        flyway     : [
            datasources: [
                default: [
                    enabled: true
                ]
            ]
        ]
    ]

    void 'test flyway migrations are executed with GORM'() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(flatten(flywayMap) as Map<String, Object>, Environment.TEST)

        when:
        applicationContext.getBean(GormMigrationRunner)

        then:
        noExceptionThrown()

        when:
        applicationContext.getBean(Flyway)

        then:
        noExceptionThrown()

        when:
        FlywayConfigurationProperties config = applicationContext.getBean(FlywayConfigurationProperties)

        then:
        noExceptionThrown()
        !config.isAsync()

        when:
        Map m = new Yaml().load(cleanYamlAsciidocTag(gormConfig))

        then:
        m == flywayMap

        when:
        Map db = [url: 'jdbc:h2:mem:GORMDb', user: 'sa', password: '', driver: 'org.h2.Driver']
        Sql sql = Sql.newInstance(db.url, db.user, db.password, db.driver)

        then:
        sql.rows('select count(*) from books').get(0)[0] == 2
    }
}
