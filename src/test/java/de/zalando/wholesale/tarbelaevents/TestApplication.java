package de.zalando.wholesale.tarbelaevents;

import de.zalando.wholesale.tarbelaevents.util.Fixture;
import de.zalando.wholesale.tarbelaevents.util.MockPayload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Collection;
import java.util.stream.Stream;

@SpringBootApplication
@EnableTarbelaEvents
public class TestApplication {

    private static volatile Collection<MockPayload> list = Fixture.mockPayloadList(6);

    /**
     * Test implementation of the TarbelaSnapshotProvider interface for integration tests
     */
    @Bean
    public TarbelaSnapshotProvider<MockPayload> tarbelaSnapshotProvider() {
        return () -> list.stream();
    }

    public static void setList(Collection<MockPayload> newList) {
        list = newList;
    }

    public static void main(final String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
