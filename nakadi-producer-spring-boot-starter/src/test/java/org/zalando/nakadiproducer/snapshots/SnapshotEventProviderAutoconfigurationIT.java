package org.zalando.nakadiproducer.snapshots;

import static org.junit.Assert.fail;

import java.util.Collections;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.nakadiproducer.BaseMockedExternalCommunicationIT;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotCreationService;

public class SnapshotEventProviderAutoconfigurationIT extends BaseMockedExternalCommunicationIT {

    @Autowired
    SnapshotCreationService snapshotCreationService;

    @Test
    public void picksUpDefinedSnapshotEventProviders() {
        // expect no exceptions
        snapshotCreationService.createSnapshotEvents("A");

        // expect no exceptions
        snapshotCreationService.createSnapshotEvents("B");

        try {
            snapshotCreationService.createSnapshotEvents("not defined");
        } catch (UnknownEventTypeException e) {
            return;
        }

        fail("unknown event type did not result in an exception");
    }

    @Configuration
    public static class Config {

        @Bean
        public SnapshotEventProvider snapshotEventProviderA() {
            return new SimpleSnapshotEventProvider("A", (x) -> Collections.emptyList());
        }

        @Bean
        public SnapshotEventProvider snapshotEventProviderB() {
            return new SimpleSnapshotEventProvider("B", (x) -> Collections.emptyList());
        }
    }
}
