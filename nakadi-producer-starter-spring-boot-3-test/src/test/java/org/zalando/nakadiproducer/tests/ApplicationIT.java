package org.zalando.nakadiproducer.tests;

import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.context.junit4.SpringRunner;
import org.zalando.nakadiproducer.transmission.MockNakadiPublishingClient;
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
        classes = { Application.class, MockNakadiConfig.class },
        properties = { "nakadi-producer.transmission-polling-delay=30"},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class ApplicationIT {
    @LocalManagementPort
    private int localManagementPort;

    @ClassRule
    public static final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @BeforeClass
    public static void fakeCredentialsDir() {
        environmentVariables.set("CREDENTIALS_DIR", new File("src/main/test/tokens").getAbsolutePath());
    }

    @Autowired
    private MockNakadiPublishingClient mockClient;

    @Autowired
    private EventTransmitter eventTransmitter;

    @Before
    @After
    public void cleanUpMock() {
        eventTransmitter.sendEvents();
        mockClient.clearSentEvents();
    }

    @Test
    public void shouldSuccessfullyStartAndSnapshotCanBeTriggered() throws InterruptedException {
        given().baseUri("http://localhost:" + localManagementPort).contentType("application/json")
        .when().post("/actuator/snapshot-event-creation/eventtype")
        .then().statusCode(204);

        // leave some time for the scheduler to run
        Thread.sleep(200);

        List<String> events = mockClient.getSentEvents("eventtype");
        assertThat(events, hasSize(2));
    }


}
