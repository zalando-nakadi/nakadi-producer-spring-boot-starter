package org.zalando.nakadiproducer;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.flywaydb.core.api.callback.Event.*;

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

    @Autowired
    private FlywayProperties flywayProperties;

    @Autowired
    private DataSourceProperties dataSourceProperties;

    @PostConstruct
    public void migrateFlyway() {
        final FluentConfiguration config = Flyway.configure();

        if (this.nakadiProducerFlywayDataSource != null) {
            config.dataSource(nakadiProducerFlywayDataSource);
//        } else if (this.flywayProperties != null && this.flywayProperties.isCreateDataSource()) {
        } else if (this.flywayProperties != null && flywayProperties.getUser() != null && flywayProperties.getUrl() != null) {
            config.dataSource(
                    Optional.ofNullable(this.flywayProperties.getUrl()).orElse(dataSourceProperties.getUrl()),
                    Optional.ofNullable(this.flywayProperties.getUser()).orElse(dataSourceProperties.getUsername()),
                    Optional.ofNullable(this.flywayProperties.getPassword()).orElse(dataSourceProperties.getPassword()));

            config.initSql(String.join(";\n", flywayProperties.getInitSqls()));
        } else if (this.flywayDataSource != null) {
            config.dataSource(this.flywayDataSource);
        } else {
            config.dataSource(dataSource);
        }

        config.locations("classpath:db_nakadiproducer/migrations");
        config.schemas("nakadi_events");
        if (callbacks != null) {
            config.callbacks(callbacks.stream().map(FlywayCallbackAdapter::new).toArray(FlywayCallbackAdapter[]::new));
        }

        config.baselineOnMigrate(true);
        config.baselineVersion("2133546886.1.0");

        Flyway flyway = new Flyway(config);
        flyway.migrate();
    }

    private static class FlywayCallbackAdapter extends BaseCallback {

        private final NakadiProducerFlywayCallback callback;

        private final Set<Event> supportedCallbacks = Stream.of(
                BEFORE_CLEAN,
                AFTER_CLEAN,
                BEFORE_MIGRATE,
                BEFORE_EACH_MIGRATE,
                AFTER_EACH_MIGRATE,
                AFTER_MIGRATE,
                BEFORE_UNDO,
                BEFORE_EACH_UNDO,
                AFTER_EACH_UNDO,
                AFTER_UNDO,
                BEFORE_VALIDATE,
                AFTER_VALIDATE,
                BEFORE_BASELINE,
                AFTER_BASELINE,
                BEFORE_REPAIR,
                AFTER_REPAIR,
                BEFORE_INFO,
                AFTER_INFO

        ).collect(Collectors.toSet());

        private FlywayCallbackAdapter(NakadiProducerFlywayCallback callback) {
            this.callback = callback;
        }

        @Override
        public boolean supports(Event event, Context context) {
            return supportedCallbacks.contains(event);
        }

        @Override
        public void handle(Event event, Context context) {
            switch (event) {
                case BEFORE_CLEAN:
                    callback.beforeClean(context.getConnection());
                    break;
                case AFTER_CLEAN:
                    callback.afterClean(context.getConnection());
                    break;
                case BEFORE_MIGRATE:
                    callback.beforeMigrate(context.getConnection());
                    break;
                case BEFORE_EACH_MIGRATE:
                    callback.beforeEachMigrate(context.getConnection(), context.getMigrationInfo());
                    break;
                case AFTER_EACH_MIGRATE:
                    callback.afterEachMigrate(context.getConnection(), context.getMigrationInfo());
                    break;
                case AFTER_MIGRATE:
                    callback.afterMigrate(context.getConnection());
                    break;
                case BEFORE_UNDO:
                    callback.beforeUndo(context.getConnection());
                    break;
                case BEFORE_EACH_UNDO:
                    callback.beforeEachUndo(context.getConnection(), context.getMigrationInfo());
                    break;
                case AFTER_EACH_UNDO:
                    callback.afterEachUndo(context.getConnection(), context.getMigrationInfo());
                    break;
                case AFTER_UNDO:
                    callback.afterUndo(context.getConnection());
                    break;
                case BEFORE_VALIDATE:
                    callback.beforeValidate(context.getConnection());
                    break;
                case AFTER_VALIDATE:
                    callback.afterValidate(context.getConnection());
                    break;
                case BEFORE_BASELINE:
                    callback.beforeBaseline(context.getConnection());
                    break;
                case AFTER_BASELINE:
                    callback.afterBaseline(context.getConnection());
                    break;
                case BEFORE_REPAIR:
                    callback.beforeRepair(context.getConnection());
                    break;
                case AFTER_REPAIR:
                    callback.afterRepair(context.getConnection());
                    break;
                case BEFORE_INFO:
                    callback.beforeInfo(context.getConnection());
                    break;
                case AFTER_INFO:
                    callback.afterInfo(context.getConnection());
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + event);
            }
        }
    }
}
