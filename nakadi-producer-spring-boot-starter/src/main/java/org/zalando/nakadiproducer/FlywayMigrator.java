package org.zalando.nakadiproducer;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.FlywayCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.stereotype.Component;

@Component
public class FlywayMigrator {
    @Autowired(required = false)
    @NakadiProducerFlywayDataSource
    private DataSource nakadiProducerFlywayDataSource;

    @Autowired(required = false)
    @FlywayDataSource
    private DataSource flywayDataSource;

    @Autowired
    private DataSource dataSource;

    @Autowired
    @NakadiProducerFlywayCallback
    private FlywayCallback callback;

    @PostConstruct
    public void migrateFlyway() {
        DataSource effectiveDataSource = this.nakadiProducerFlywayDataSource;

        if (effectiveDataSource == null) {
            effectiveDataSource = flywayDataSource;
        }

        if (effectiveDataSource == null) {
            effectiveDataSource = dataSource;
        }

        Flyway flyway = new Flyway();
        flyway.setLocations("classpath:db_nakadiproducer/migrations");
        flyway.setSchemas("nakadi_events");
        flyway.setDataSource(effectiveDataSource);
        flyway.setCallbacks(callback);
        flyway.setBaselineOnMigrate(true);
        flyway.setBaselineVersionAsString("2133546886.1.0");
        flyway.migrate();
    }
}
