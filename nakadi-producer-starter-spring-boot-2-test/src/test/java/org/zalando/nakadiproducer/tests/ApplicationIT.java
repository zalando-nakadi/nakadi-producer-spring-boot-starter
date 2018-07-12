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
        classes = Application.class,
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
        environmentVariables.set("CREDENTIALS_DIR", new File("src/main/resources/tokens").getAbsolutePath());
    }

    @Test
    public void shouldSuccessfullyStartAndSnapshotCanBeTriggered() {
        given().baseUri("http://localhost:" + localManagementPort).contentType("application/json")
        .when().post("/actuator/snapshot-event-creation/eventtype")
        .then().statusCode(204);
    }


}
