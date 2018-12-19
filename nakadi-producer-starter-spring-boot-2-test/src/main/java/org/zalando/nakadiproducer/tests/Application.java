package org.zalando.nakadiproducer.tests;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.zalando.nakadiproducer.snapshots.SimpleSnapshotEventGenerator;
import org.zalando.nakadiproducer.snapshots.Snapshot;
import org.zalando.nakadiproducer.snapshots.SnapshotEventGenerator;

import javax.sql.DataSource;

import java.io.IOException;
import java.util.Collections;

@EnableAutoConfiguration
public class Application {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @Primary
    public DataSource dataSource(EmbeddedPostgres postgres) throws IOException {
        return postgres.getPostgresDatabase();
    }

    @Bean
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.start();
    }

    @Bean
    public SnapshotEventGenerator snapshotEventGenerator() {
        return new SimpleSnapshotEventGenerator("eventtype", (withIdGreaterThan, filter) -> {
            if (withIdGreaterThan == null) {
                return Collections.singletonList(new Snapshot("1", "foo", new Data("1", filter)));
            } else if (withIdGreaterThan.equals("1")) {
                return Collections.singletonList(new Snapshot("2", "foo", new Data("2", filter)));
            } else {
                return Collections.emptyList();
            }
        });

        // Todo: Test that some events arrive at a local nakadi mock
    }

    public static class Data {
        public String id;
        public String filter;
        public Data(String id, String filter) {
            super();
            this.id = id;
            this.filter = filter;
        }
    }
}
