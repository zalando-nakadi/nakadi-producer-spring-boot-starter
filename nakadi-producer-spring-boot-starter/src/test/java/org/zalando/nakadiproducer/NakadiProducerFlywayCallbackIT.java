package org.zalando.nakadiproducer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Connection;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;

public class NakadiProducerFlywayCallbackIT extends BaseMockedExternalCommunicationIT {

    @MockitoBean
    private NakadiProducerFlywayCallback flywayCallback;

    @MockitoBean
    private ConfigurationAwareNakadiProducerFlywayCallback configurationAwareNakadiProducerFlywayCallback;

    @Test
    @DirtiesContext // Needed to make sure that flyway gets executed for each of the tests and Callbacks are called again
    public void flywayCallbackIsCalledIfAnnotatedWithQualifierAnnotation() {
        verify(flywayCallback, times(1)).beforeMigrate(any(Connection.class));
    }

    @Test
    @DirtiesContext // Needed to make sure that flyway gets executed for each of the tests and Callbacks are called again
    public void flywayConfigurationIsSetIfCallbackIsConfigurationAware() {
        verify(configurationAwareNakadiProducerFlywayCallback, times(1)).beforeMigrate(any(Connection.class));
    }

    public interface ConfigurationAwareNakadiProducerFlywayCallback extends NakadiProducerFlywayCallback {
    }
}
