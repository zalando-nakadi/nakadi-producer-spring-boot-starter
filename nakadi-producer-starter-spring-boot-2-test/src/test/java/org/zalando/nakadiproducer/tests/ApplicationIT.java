package org.zalando.nakadiproducer.tests;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;

import static io.restassured.RestAssured.given;

@RunWith(SpringRunner.class)
@SpringBootTest(
        // This line looks like that by intention: We want to test that the MockNakadiPublishingClient will be picked up
        // by our starter *even if* it has been defined *after* the application itself. This has been a problem until
        // this commit.
        classes = { Application.class, MockNakadiConfig.class },
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

    @Test
    public void shouldSuccessfullyStartAndSnapshotCanBeTriggered() {
        given().baseUri("http://localhost:" + localManagementPort).contentType("application/json")
        .when().post("/actuator/snapshot-event-creation/eventtype")
        .then().statusCode(204);
    }


}
