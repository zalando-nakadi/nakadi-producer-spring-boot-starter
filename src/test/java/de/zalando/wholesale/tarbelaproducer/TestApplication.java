package de.zalando.wholesale.tarbelaproducer;

import de.zalando.wholesale.tarbelaproducer.service.exception.UnknownEventTypeException;
import de.zalando.wholesale.tarbelaproducer.util.Fixture;
import de.zalando.wholesale.tarbelaproducer.util.MockPayload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Collection;
import java.util.Objects;

import static de.zalando.wholesale.tarbelaproducer.util.Fixture.PUBLISHER_EVENT_OTHER_TYPE;
import static de.zalando.wholesale.tarbelaproducer.util.Fixture.PUBLISHER_EVENT_TYPE;
import static de.zalando.wholesale.tarbelaproducer.util.Fixture.mockEventPayload;

@SpringBootApplication
@EnableTarbelaProducer
public class TestApplication {

    private static volatile Collection<MockPayload> list = Fixture.mockPayloadList(6);

    /**
     * Test implementation of the TarbelaSnapshotProvider interface for integration tests
     */
    @Bean
    public TarbelaSnapshotProvider tarbelaSnapshotProvider() {
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
