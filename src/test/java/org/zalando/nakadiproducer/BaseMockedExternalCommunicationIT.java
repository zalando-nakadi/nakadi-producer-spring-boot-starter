package org.zalando.nakadiproducer;

import com.jayway.restassured.specification.RequestSpecification;

import org.zalando.nakadiproducer.config.EmbeddedDataSourceConfig;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static com.jayway.restassured.RestAssured.given;


@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = { "server.port:50203", "management.port:0", "zalando.team.id:alpha-local-testing" },
        classes = { TestApplication.class, EmbeddedDataSourceConfig.class }
)
public abstract class BaseMockedExternalCommunicationIT {

    @Value("${local.server.port}")
    int httpPort;

    protected RequestSpecification aHttpsRequest() {
        return given().relaxedHTTPSValidation().baseUri("http://localhost:" + httpPort);
    }
}
