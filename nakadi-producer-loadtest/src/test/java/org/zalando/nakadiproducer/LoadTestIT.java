package org.zalando.nakadiproducer;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;

public class LoadTestIT {

    @ClassRule
    public static DockerComposeContainer compose =
            new DockerComposeContainer(new File("src/test/resources/docker-compose.yaml")).
                    withExposedService("nakadi", 8080,
                            Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))
                    .withExposedService("nakadi-postgres", 5432,
                            Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));

    @Before
    public void init() {
        // TODO
    }

    @Test
    public void testThatUsesSomeDockerServices() {
        // TODO
    }
}