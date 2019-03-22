package org.zalando.nakadiproducer;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.zalando.nakadiproducer.configuration.AopConfiguration;
import org.zalando.nakadiproducer.configuration.TokenConfiguration;
import org.zalando.nakadiproducer.event.ExampleBusinessEvent;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.interceptor.ProfilerInterceptor;
import org.zalando.nakadiproducer.transmission.impl.EventTransmitter;

import java.io.File;
import java.time.Duration;
import java.util.stream.IntStream;

import static org.zalando.nakadiproducer.event.ExampleBusinessEvent.EVENT_NAME;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {"nakadi-producer.scheduled-transmission-enabled=false"})
@ContextConfiguration(classes = {Application.class, TokenConfiguration.class, AopConfiguration.class, ProfilerInterceptor.class})
@Slf4j
public class LoadTestIT {

    @ClassRule
    public static DockerComposeContainer compose =
            new DockerComposeContainer(new File("src/test/resources/docker-compose.yaml"))
                    .withExposedService("nakadi", 8080,
                            Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(240)));

    @Autowired
    private EventLogWriter eventLogWriter;

    @Autowired
    private EventTransmitter eventTransmitter;

    @Autowired
    private RestTemplate restTemplate;

    @Before
    public void init() {
        createExampleEvent();
    }

    private void createExampleEvent() {
        String createEvent =
                "{\n" +
                "  \"name\": \"" + EVENT_NAME + "\",\n" +
                "  \"owning_application\": \"nakadi-producer-loadtest\",\n" +
                "  \"category\": \"undefined\",\n" +
                "  \"partition_strategy\": \"random\",\n" +
                "  \"schema\": {\n" +
                "    \"type\": \"json_schema\",\n" +
                "    \"schema\": \"{ \\\"properties\\\": { \\\"content\\\": { \\\"type\\\": \\\"string\\\" } } }\"\n" +
                "  }" +
                "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(createEvent, headers);

        ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080/event-types", HttpMethod.POST, entity, String.class);
        log.debug("created event {}, response [{}]", EVENT_NAME, response);
    }

    @After
    public void cleanup() {
        deleteExampleEvent();
    }

    private void deleteExampleEvent() {
        restTemplate.delete("http://localhost:8080/event-types/" + EVENT_NAME);
    }

    @Test
    public void testFireAndSendEvents10k() {
        fireBusinessEvents(10_000);
        sendEvents();
    }

    @Test
    public void testFireAndSendEvents50k() {
        fireBusinessEvents(50_000);
        sendEvents();
    }

    @Test
    public void testFireAndSendEvents100k() {
        fireBusinessEvents(100_000);
        sendEvents();
    }

    @Test
    public void testFireAndSendEvents300k() {
        fireBusinessEvents(300_000);
        sendEvents();
    }

    private void fireBusinessEvents(int amount) {
      log.info("=== Starting to fire " + amount + " events ============================================================");
        IntStream.rangeClosed(1, amount).forEach(
                i -> {
                    ExampleBusinessEvent event = new ExampleBusinessEvent("example-business-event " + i + " of " + amount);
                    eventLogWriter.fireBusinessEvent(EVENT_NAME, event);
                    log.debug("fired event: [{}]", event);
                }
        );
    }

    private void sendEvents() {
        eventTransmitter.sendEvents();
    }
}