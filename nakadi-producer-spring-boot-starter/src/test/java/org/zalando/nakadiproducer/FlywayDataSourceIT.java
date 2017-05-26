package org.zalando.nakadiproducer;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;

public class FlywayDataSourceIT extends BaseMockedExternalCommunicationIT {
    @Autowired
    @NakadiProducerFlywayDataSource
    DataSource dataSource;

    @Test
    public void usesNakadiProducerDataSourceIfAnnotatedWithQualifier() throws SQLException {
        verify(dataSource, atLeastOnce()).getConnection();
    }

    @Configuration
    public static class Config {
        @Autowired
        EmbeddedPostgres embeddedPostgres;

        @Bean
        @NakadiProducerFlywayDataSource
        public DataSource flywayDataSource() {
            return Mockito.spy(embeddedPostgres.getPostgresDatabase());
        }
    }

}
