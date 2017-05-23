package org.zalando.nakadiproducer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.Connection;

import org.flywaydb.core.api.callback.FlywayCallback;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

public class NonNakadiProducerFlywayCallbackIT extends BaseMockedExternalCommunicationIT {

    @MockBean
    FlywayCallback flywayCallback;

    @Test
    public void flywayCallbackIsNotUsedIfNotAnnotatedWithQualifierAnnotation() {
        verify(flywayCallback, never()).beforeValidate(any(Connection.class));
    }
}
