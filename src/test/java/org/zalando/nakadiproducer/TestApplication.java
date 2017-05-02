package org.zalando.nakadiproducer;

import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_TYPE;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider.Snapshot;
import org.zalando.nakadiproducer.snapshots.UnknownEventTypeException;
import org.zalando.nakadiproducer.util.Fixture;

@SpringBootApplication
@EnableNakadiProducer
public class TestApplication {

    private static volatile List<Snapshot> list = Fixture.mockSnapshotList(6);

    /**
     * Test implementation of the SnapshotEventProvider interface for integration tests
     */
    @Bean
    public SnapshotEventProvider snapshotEventProvider() {
        return (eventType, withIdGreaterThan) -> {
            if (!Objects.equals(eventType, PUBLISHER_EVENT_TYPE)) {
                    throw new UnknownEventTypeException(eventType);
            } else if (withIdGreaterThan != null) {
                return Collections.emptyList();
            } else {
                return list.stream()
                           .filter(snapshot -> snapshot.getEventPayload().getEventType().equals(eventType))
                           .collect(Collectors.toList());
            }
        };
    }

    public static void setList(List<Snapshot> newList) {
        list = newList;
    }

    public static void main(final String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
