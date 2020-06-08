package io.micronaut.flyway.docs

import groovy.sql.Sql
import io.micronaut.flyway.FlywayConfigurationProperties
import io.micronaut.flyway.YamlAsciidocTagCleaner
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import org.yaml.snakeyaml.Yaml
import spock.lang.Shared
import spock.lang.Specification

class GormMultipleDataSourcesSpec extends Specification implements YamlAsciidocTagCleaner {

    String gormConfig = '''\
spec.name: GormDocSpec
//tag::yamlconfig[]
dataSource: # <1>
  pooled: true
  jmxExport: true
  dbCreate: none
  url: 'jdbc:h2:mem:flywayGORMDb'
  driverClassName: org.h2.Driver
  username: sa
  password: ''
  
dataSources:
  books: # <2>
    pooled: true
    jmxExport: true
    dbCreate: none
    url: 'jdbc:h2:mem:flywayBooksDb'
    driverClassName: org.h2.Driver
    username: sa
    password: ''
        
flyway:
  datasources:
    default: # <3>
      enabled: true
    books: # <4>
      enabled: true
'''//end::yamlconfig[]

    @Shared
    Map<String, Object> flywayMap = [
        'spec.name': 'GormDocSpec',
        dataSource : [
            pooled         : true,
            jmxExport      : true,
            dbCreate       : 'none',
            url            : 'jdbc:h2:mem:flywayGORMDb',
            driverClassName: 'org.h2.Driver',
            username       : 'sa',
            password       : ''
        ],
        dataSources : [
            books: [
                pooled         : true,
                jmxExport      : true,
                dbCreate       : 'none',
                url            : 'jdbc:h2:mem:flywayBooksDb',
                driverClassName: 'org.h2.Driver',
                username       : 'sa',
                password       : ''
            ]
        ],
        flyway     : [
            datasources: [
                default: [
                    enabled: true
                ],
                books: [
                    enabled: true
                ]
            ]
        ]
    ]

    void 'test flyway migrations are executed with GORM with multiple datasources'() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(flatten(flywayMap) as Map<String, Object>, Environment.TEST)

        when:
        Collection<FlywayConfigurationProperties> configurationProperties = applicationContext.getBeansOfType(FlywayConfigurationProperties)

        then:
        configurationProperties.find { it.nameQualifier == 'default' }
        configurationProperties.find { it.nameQualifier == 'books' }

        when:
        Map m = new Yaml().load(cleanYamlAsciidocTag(gormConfig))

        then:
        m == flywayMap

        when: 'connecting to the default datasource'
        Map db = [url: 'jdbc:h2:mem:flywayGORMDb', user: 'sa', password: '', driver: 'org.h2.Driver']
        Sql sql = Sql.newInstance(db.url, db.user, db.password, db.driver)

        then: 'the migrations have been executed'
        sql.rows('select count(*) from books').get(0)[0] == 2

        when: 'connecting to another datasource'
        Map db2 = [url: 'jdbc:h2:mem:flywayBooksDb', user: 'sa', password: '', driver: 'org.h2.Driver']
        Sql sql2 = Sql.newInstance(db2.url, db2.user, db2.password, db2.driver)

        then: 'the migrations have been executed'
        sql2.rows('select count(*) from books').get(0)[0] == 2
    }
}
