package org.zalando.nakadiproducer.snapshots;

import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.nakadiproducer.BaseMockedExternalCommunicationIT;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotCreationService;

public class SnapshotEventGeneratorAutoconfigurationIT extends BaseMockedExternalCommunicationIT {

    @Autowired
    SnapshotCreationService snapshotCreationService;

    @Test
    public void picksUpDefinedSnapshotEventProviders() {
        // expect no exceptions
        snapshotCreationService.createSnapshotEvents("A");

        // expect no exceptions
        snapshotCreationService.createSnapshotEvents("B");

        // expect no exceptions
        snapshotCreationService.createSnapshotEvents("C");

        // expect no exceptions
        snapshotCreationService.createSnapshotEvents("D");

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
        public SnapshotEventGenerator snapshotEventProviderA() {
            return new SimpleSnapshotEventGenerator("A", (x) -> Collections.emptyList());
        }

        @Bean
        public SnapshotEventGenerator snapshotEventProviderB() {
            return new SimpleSnapshotEventGenerator("B", (x) -> Collections.emptyList());
        }

        @Bean
        public SnapshotEventProvider snapshotEventProviderCandD() {
            return new SnapshotEventProvider() {
                @Override
                public List<Snapshot> getSnapshot(String eventType, Object withIdGreaterThan) {
                    if (!getSupportedEventTypes().contains(eventType)) {
                        throw new UnknownEventTypeException(eventType);
                    }
                    return Collections.emptyList();
                }

                @Override
                public Set<String> getSupportedEventTypes() {
                    HashSet<String> types = new HashSet<>();
                    types.add("C");
                    types.add("D");
                    return types;
                }
            };
        }
    }
}
