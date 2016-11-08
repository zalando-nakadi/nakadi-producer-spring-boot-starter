package de.zalando.wholesale.tarbelaevents;

import de.zalando.wholesale.tarbelaevents.util.Fixture;
import de.zalando.wholesale.tarbelaevents.util.MockPayload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Collection;

@SpringBootApplication
@EnableTarbelaEvents
public class TestApplication {

    private static volatile Collection<MockPayload> list = Fixture.mockPayloadList(6);

    /**
     * Test implementation of the TarbelaSnapshotProvider interface for integration tests
     */
    @Bean
    public TarbelaSnapshotProvider tarbelaSnapshotProvider() {
        return eventType -> list.stream().map(Fixture::mockEventPayload)
                .filter(eventPayload -> eventPayload.getEventType().equals(eventType));
    }

    public static void setList(Collection<MockPayload> newList) {
        list = newList;
    }

    public static void main(final String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
