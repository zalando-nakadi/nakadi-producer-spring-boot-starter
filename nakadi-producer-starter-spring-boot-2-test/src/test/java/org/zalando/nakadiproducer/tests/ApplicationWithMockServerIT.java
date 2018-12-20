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
import org.zalando.nakadi_mock.EventSubmissionCallback.DataChangeEvent;
import org.zalando.nakadi_mock.NakadiMock;
import org.zalando.nakadiproducer.tests.Application.Data;
import java.io.File;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = { Application.class },
        properties = { "nakadi-producer.transmission-polling-delay=30"},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers=NakadiServerMockInitializer.class)
public class ApplicationWithMockServerIT {

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
    NakadiMock nakadiMock;

    @Test
    public void shouldSuccessfullyStartAndSnapshotCanBeTriggered() throws InterruptedException {
        CollectingCallback<DataChangeEvent<Data>> collector = new CollectingCallback<DataChangeEvent<Application.Data>>() {};
        nakadiMock.eventType("eventtype").setSubmissionCallback(collector);

        given().baseUri("http://localhost:" + localManagementPort)
               .contentType("application/json")
               .body("{'filter':'Example filter'}".replace('\'', '"'))
        .when().post("/actuator/snapshot-event-creation/eventtype")
        .then().statusCode(204);

        Thread.sleep(1200);

        List<DataChangeEvent<Data>> events = collector.getSubmittedEvents();
        assertThat(events, hasSize(2));
        assertThat(events.get(0).getDataOp(), is("S"));
        assertThat(events.get(0).getData().id, is("1"));

        assertThat(events.get(1).getDataOp(), is("S"));
        assertThat(events.get(1).getData().id, is("2"));
    }
}
