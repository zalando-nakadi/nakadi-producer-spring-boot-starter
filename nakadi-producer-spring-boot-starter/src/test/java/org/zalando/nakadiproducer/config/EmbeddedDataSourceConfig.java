package org.zalando.nakadiproducer.config;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

import javax.sql.DataSource;

@Configuration
public class EmbeddedDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() throws IOException {
        return embeddedPostgres().getPostgresDatabase();
    }

    @Bean
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.start();
    }
}
