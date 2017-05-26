package org.zalando.nakadiproducer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Connection;

import org.flywaydb.core.api.callback.FlywayCallback;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

public class NakadiProducerFlywayCallbackIT extends BaseMockedExternalCommunicationIT {

    @MockBean
    @NakadiProducerFlywayCallback
    FlywayCallback flywayCallback;

    @Test
    public void flywayCallbackIsCalledIfAnnotatedWithQualifierAnnotation() {
        verify(flywayCallback, times(1)).beforeValidate(any(Connection.class));
    }
}
