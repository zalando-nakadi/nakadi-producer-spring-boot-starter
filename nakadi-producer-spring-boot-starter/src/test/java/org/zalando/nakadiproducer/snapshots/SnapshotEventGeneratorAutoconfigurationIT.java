package org.zalando.nakadiproducer.snapshots;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.zalando.nakadiproducer.BaseMockedExternalCommunicationIT;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotCreationService;

import java.util.Collections;

import static org.assertj.core.api.Fail.fail;


@ContextConfiguration(classes = SnapshotEventGeneratorAutoconfigurationIT.Config.class)
public class SnapshotEventGeneratorAutoconfigurationIT extends BaseMockedExternalCommunicationIT {

    @Autowired
    private SnapshotCreationService snapshotCreationService;

    @Test
    public void picksUpDefinedSnapshotEventProviders() {
        // expect no exceptions
        snapshotCreationService.createSnapshotEvents("A", "");

        // expect no exceptions
        snapshotCreationService.createSnapshotEvents("B", "");

        try {
            snapshotCreationService.createSnapshotEvents("not defined", "");
        } catch (final UnknownEventTypeException e) {
            return;
        }

        fail("unknown event type did not result in an exception");
    }

    @Configuration
    public static class Config {

        @Bean
        public SnapshotEventGenerator snapshotEventProviderA() {
            return new SimpleSnapshotEventGenerator("A", (x) -> Collections.emptyList());
        }

        @Bean
        public SnapshotEventGenerator snapshotEventProviderB() {
            return new SimpleSnapshotEventGenerator("B", (x) -> Collections.emptyList());
        }
    }
}
