package de.zalando.wholesale.tarbelaevents.config;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

import javax.sql.DataSource;

@Configuration
public class EmbeddedDataSourceConfig {

    @Bean
    public DataSource dataSource() throws IOException {
        return embeddedPostgres().getPostgresDatabase();
    }

    @Bean
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.start();
    }
}
