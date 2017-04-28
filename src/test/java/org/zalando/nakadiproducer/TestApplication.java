package org.zalando.nakadiproducer;

import org.zalando.nakadiproducer.service.exception.UnknownEventTypeException;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Collection;
import java.util.Objects;

import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_OTHER_TYPE;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_TYPE;
import static org.zalando.nakadiproducer.util.Fixture.mockEventPayload;

@SpringBootApplication
@EnableNakadiProducer
public class TestApplication {

    private static volatile Collection<MockPayload> list = Fixture.mockPayloadList(6);

    /**
     * Test implementation of the SnapshotEventProvider interface for integration tests
     */
    @Bean
    public SnapshotEventProvider snapshotEventProvider() {
        return eventType -> {
            if (Objects.equals(eventType, PUBLISHER_EVENT_TYPE)) {
                return list.stream().map(Fixture::mockEventPayload)
                        .filter(eventPayload -> eventPayload.getEventType().equals(eventType));
            } else if (Objects.equals(eventType, PUBLISHER_EVENT_OTHER_TYPE)) {
                return list.stream().map(it -> mockEventPayload(it, PUBLISHER_EVENT_OTHER_TYPE))
                        .filter(eventPayload -> eventPayload.getEventType().equals(eventType));
            } else {
                throw new UnknownEventTypeException(eventType);
            }
        };
    }

    public static void setList(Collection<MockPayload> newList) {
        list = newList;
    }

    public static void main(final String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
