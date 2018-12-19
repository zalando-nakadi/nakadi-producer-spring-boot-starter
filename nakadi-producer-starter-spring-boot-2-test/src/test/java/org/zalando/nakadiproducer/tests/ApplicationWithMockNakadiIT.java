package org.zalando.nakadiproducer.tests;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.zalando.nakadi_mock.EventSubmissionCallback.CollectingCallback;
import org.zalando.nakadi_mock.NakadiMock;
import org.zalando.nakadiproducer.tests.Application.Data;
import org.zalando.nakadiproducer.transmission.impl.EventTransmitter;

import java.io.File;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        // This line looks like that by intention: We want to test that the MockNakadiPublishingClient will be picked up
        // by our starter *even if* it has been defined *after* the application itself. This has been a problem until
        // this commit.
        classes = { MockNakadiServerConfig.class, Application.class },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers=MockNakadiServerConfig.MockPropertyInitializer.class)
public class ApplicationWithMockNakadiIT {
    @LocalManagementPort
    private int localManagementPort;

    @ClassRule
    public static final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @BeforeClass
    public static void fakeCredentialsDir() {
        environmentVariables.set("CREDENTIALS_DIR", new File("src/test/resources/tokens").getAbsolutePath());
    }

    @Autowired
    EventTransmitter transmitter;

    @Autowired
    NakadiMock nakadiMock;

    @Test
    public void shouldSuccessfullyStartAndSnapshotCanBeTriggered() throws InterruptedException {
        CollectingCallback<Application.Data> collector = new CollectingCallback<Application.Data>() {};
        nakadiMock.eventType("eventtype").setSubmissionCallback(collector);

        given().baseUri("http://localhost:" + localManagementPort)
               .contentType("application/json")
               .body("{'filter':'Example filter'}".replace('\'', '"'))
        .when().post("/actuator/snapshot-event-creation/eventtype")
        .then().statusCode(204);

        Thread.sleep(500);

        transmitter.sendEvents();

        Thread.sleep(500);

        List<Data> events = collector.getSubmittedEvents();
        assertThat(events, hasSize(2));
    }
}
