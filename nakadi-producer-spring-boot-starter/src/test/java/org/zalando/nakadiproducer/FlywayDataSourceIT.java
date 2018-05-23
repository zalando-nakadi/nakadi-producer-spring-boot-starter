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
import org.springframework.test.context.ContextConfiguration;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;

@ContextConfiguration(classes=FlywayDataSourceIT.Config.class)
public class FlywayDataSourceIT extends BaseMockedExternalCommunicationIT {
    @Autowired
    @NakadiProducerFlywayDataSource
    private DataSource dataSource;

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
