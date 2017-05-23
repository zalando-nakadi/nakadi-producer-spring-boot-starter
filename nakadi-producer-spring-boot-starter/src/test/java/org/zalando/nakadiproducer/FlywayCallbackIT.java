package org.zalando.nakadiproducer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.sql.Connection;

import org.flywaydb.core.api.callback.FlywayCallback;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

public class FlywayCallbackIT extends BaseMockedExternalCommunicationIT {

    @MockBean
    @NakadiProducerFlywayCallback
    FlywayCallback flywayCallback;

    @Test
    public void flywayCallbackIsCalledIfAnnotatedWithQualifierAnnotation() {
        verify(flywayCallback, atLeastOnce()).beforeValidate(any(Connection.class));
    }
}