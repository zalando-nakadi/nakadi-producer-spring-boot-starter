package org.zalando.nakadiproducer;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.FlywayCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;

public class FlywayMigrator {
    @Autowired(required = false)
    @NakadiProducerFlywayDataSource
    private DataSource nakadiProducerFlywayDataSource;

    @Autowired(required = false)
    @FlywayDataSource
    private DataSource flywayDataSource;

    @Autowired
    private DataSource dataSource;

    @Autowired(required = false)
    @NakadiProducerFlywayCallback
    private FlywayCallback callback;

    @Autowired
    private FlywayProperties flywayProperties;

    @PostConstruct
    public void migrateFlyway() {
        Flyway flyway = new Flyway();

        if (this.nakadiProducerFlywayDataSource != null) {
            flyway.setDataSource(nakadiProducerFlywayDataSource);
        } else if (this.flywayProperties.isCreateDataSource()) {
            flyway.setDataSource(this.flywayProperties.getUrl(), this.flywayProperties.getUser(),
                this.flywayProperties.getPassword(),
                this.flywayProperties.getInitSqls().toArray(new String[0]));
        } else if (this.flywayDataSource != null) {
            flyway.setDataSource(this.flywayDataSource);
        } else {
            flyway.setDataSource(dataSource);
        }

        flyway.setLocations("classpath:db_nakadiproducer/migrations");
        flyway.setSchemas("nakadi_events");
        if (callback != null) {
            flyway.setCallbacks(callback);
        }
        flyway.setBaselineOnMigrate(true);
        flyway.setBaselineVersionAsString("2133546886.1.0");
        flyway.migrate();
    }
}
