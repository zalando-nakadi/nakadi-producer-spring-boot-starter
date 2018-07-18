package org.zalando.nakadiproducer;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.BaseFlywayCallback;
import org.flywaydb.core.api.callback.FlywayCallback;
import org.flywaydb.core.api.configuration.ConfigurationAware;
import org.flywaydb.core.api.configuration.FlywayConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;

import java.sql.Connection;
import java.util.List;

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
    private List<NakadiProducerFlywayCallback> callbacks;

    @Autowired(required = false)
    private FlywayProperties flywayProperties;

    @PostConstruct
    public void migrateFlyway() {
        Flyway flyway = new Flyway();

        if (this.nakadiProducerFlywayDataSource != null) {
            flyway.setDataSource(nakadiProducerFlywayDataSource);
        } else if (this.flywayProperties != null && this.flywayProperties.isCreateDataSource()) {
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
        if (callbacks != null) {
            flyway.setCallbacks(callbacks.stream().map(FlywayCallbackAdapter::new).toArray(FlywayCallback[]::new));
        }

        flyway.setBaselineOnMigrate(true);
        flyway.setBaselineVersionAsString("2133546886.1.0");
        flyway.migrate();
    }

    private static class FlywayCallbackAdapter extends BaseFlywayCallback {

        private NakadiProducerFlywayCallback callback;

        private FlywayCallbackAdapter(NakadiProducerFlywayCallback callback) {
            this.callback = callback;
        }

        @Override
        public void setFlywayConfiguration(FlywayConfiguration flywayConfiguration) {
            if (callback instanceof ConfigurationAware) {
                ((ConfigurationAware) callback).setFlywayConfiguration(flywayConfiguration);
            }
        }

        @Override
        public void beforeClean(Connection connection) {
            callback.beforeClean(connection);
        }

        @Override
        public void afterClean(Connection connection) {
            callback.afterClean(connection);
        }

        @Override
        public void beforeMigrate(Connection connection) {
            callback.beforeMigrate(connection);
        }

        @Override
        public void afterMigrate(Connection connection) {
            callback.afterMigrate(connection);
        }

        @Override
        public void beforeEachMigrate(Connection connection, MigrationInfo info) {
            callback.beforeEachMigrate(connection, info);
        }

        @Override
        public void afterEachMigrate(Connection connection, MigrationInfo info) {
            callback.afterEachMigrate(connection, info);
        }

        @Override
        public void beforeUndo(Connection connection) {
            callback.beforeUndo(connection);
        }

        @Override
        public void beforeEachUndo(Connection connection, MigrationInfo info) {
            callback.beforeEachUndo(connection, info);
        }

        @Override
        public void afterEachUndo(Connection connection, MigrationInfo info) {
            callback.afterEachUndo(connection, info);
        }

        @Override
        public void afterUndo(Connection connection) {
            callback.afterUndo(connection);
        }

        @Override
        public void beforeValidate(Connection connection) {
            callback.beforeValidate(connection);
        }

        @Override
        public void afterValidate(Connection connection) {
            callback.afterValidate(connection);
        }

        @Override
        public void beforeBaseline(Connection connection) {
            callback.beforeBaseline(connection);
        }

        @Override
        public void afterBaseline(Connection connection) {
            callback.afterBaseline(connection);
        }

        @Override
        public void beforeRepair(Connection connection) {
            callback.beforeRepair(connection);
        }

        @Override
        public void afterRepair(Connection connection) {
            callback.afterRepair(connection);
        }

        @Override
        public void beforeInfo(Connection connection) {
            callback.beforeInfo(connection);
        }

        @Override
        public void afterInfo(Connection connection) {
            callback.afterInfo(connection);
        }
    }
}
