package org.zalando.nakadiproducer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.transmission.impl.EventTransmissionService;
import org.zalando.nakadiproducer.util.Fixture;

public class EventLockSizeDefaultIT extends BaseMockedExternalCommunicationIT {

  @Autowired
  private EventLogWriter eventLogWriter;

  @Autowired
  private EventTransmissionService eventTransmissionService;

  @Test
  public void defaultEventLockSizeIsUsed() {

    for (int i = 1; i <= 8; i++) {
      eventLogWriter.fireBusinessEvent("myEventType", Fixture.mockPayload(i, "code123"));
    }

    assertThat(eventTransmissionService.lockSomeEvents(), hasSize(8));
  }

}
