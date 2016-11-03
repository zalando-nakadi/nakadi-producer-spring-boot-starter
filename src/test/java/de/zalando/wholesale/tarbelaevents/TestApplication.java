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

    @Bean
    public TarbelaSnapshotProvider<?> tarbelaSnapshotProvider() {

        return new TarbelaSnapshotProvider<MockPayload>() {
            @Override
            public Collection<MockPayload> getSnapshot() {
                return list;
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
