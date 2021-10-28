package org.zalando.nakadiproducer;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.sql.Connection;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class NakadiProducerFlywayCallbackIT extends BaseMockedExternalCommunicationIT {

    @MockBean
    private NakadiProducerFlywayCallback flywayCallback;

    @MockBean
    private ConfigurationAwareNakadiProducerFlywayCallback configurationAwareNakadiProducerFlywayCallback;

    @Test
    @DirtiesContext // Needed to make sure that flyway gets executed for each of the tests and Callbacks are called again
    public void flywayCallbackIsCalledIfAnnotatedWithQualifierAnnotation() {
        verify(flywayCallback, times(1)).beforeMigrate(any(Connection.class));
    }

    @Test
    @DirtiesContext // Needed to make sure that flyway gets executed for each of the tests and Callbacks are called again
    public void flywayConfigurationIsSetIfCallbackIsConfigurationAware() {
        InOrder inOrder = inOrder(configurationAwareNakadiProducerFlywayCallback);
        inOrder.verify(configurationAwareNakadiProducerFlywayCallback, times(1)).beforeMigrate(any(Connection.class));

    }

    public interface ConfigurationAwareNakadiProducerFlywayCallback extends NakadiProducerFlywayCallback {
    }
}
