package org.zalando.nakadiproducer.snapshots;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.LocalManagementPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.zalando.nakadiproducer.TestApplication;
import org.zalando.nakadiproducer.config.EmbeddedDataSourceConfig;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = { "management.security.enabled=false", "zalando.team.id:alpha-local-testing", "nakadi-producer.scheduled-transmission-enabled:false" },
    classes = { TestApplication.class, EmbeddedDataSourceConfig.class }
)
public class SnapshotEventGenerationWebEndpointIT {

    private static final String MY_EVENT_TYPE = "my.event-type";
    private static final String MY_REQUEST_BODY = "my request body";

    @LocalManagementPort
    private int managementPort;

    @Autowired
    private SnapshotEventGenerator snapshotEventGenerator;

    @Test
    public void passesRequestBodyIfPresent() {
        given().baseUri("http://localhost:" + managementPort)
               .body(MY_REQUEST_BODY)
        .when().post("/snapshot_event_creation/" + MY_EVENT_TYPE)
        .then().statusCode(200);

        verify(snapshotEventGenerator).generateSnapshots(null, MY_REQUEST_BODY);
    }

    @Test
    public void passesNullIfNoRequestBodyPresent() {
        given().baseUri("http://localhost:" + managementPort)
               .when().post("/snapshot_event_creation/" + MY_EVENT_TYPE)
               .then().statusCode(200);

        verify(snapshotEventGenerator).generateSnapshots(null, null);
    }

    @Configuration
    public static class Config {
        @Bean
        public SnapshotEventGenerator snapshotEventGenerator() {
            SnapshotEventGenerator mock = mock(SnapshotEventGenerator.class);
            when(mock.getSupportedEventType()).thenReturn(MY_EVENT_TYPE);
            return mock;
        }

    }
}
