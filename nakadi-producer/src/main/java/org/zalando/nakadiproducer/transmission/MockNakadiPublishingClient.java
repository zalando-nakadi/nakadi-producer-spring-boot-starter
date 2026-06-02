package org.zalando.nakadiproducer.transmission;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;

public class MockNakadiPublishingClient implements NakadiPublishingClient {
    private final ObjectMapper objectMapper;
    private final MultiValueMap<String, String> sentEvents = new LinkedMultiValueMap<>();

    public MockNakadiPublishingClient() {
        this(createDefaultObjectMapper());
    }

    public MockNakadiPublishingClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public synchronized void publish(String eventType, List<?> nakadiEvents) throws Exception {
        nakadiEvents.stream().map(this::transformToJson).forEach(e -> sentEvents.add(eventType, e));
    }

    public synchronized List<String> getSentEvents(String eventType) {
        ArrayList<String> events = new ArrayList<>();
        List<String> sentEvents = this.sentEvents.get(eventType);
        if (sentEvents != null) {
            events.addAll(sentEvents);
        }
        return events;
    }

    public synchronized void clearSentEvents() {
        sentEvents.clear();
    }

    private String transformToJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    private static ObjectMapper createDefaultObjectMapper() {
        return JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .changeDefaultPropertyInclusion(i -> i.withValueInclusion(JsonInclude.Include.NON_NULL))
.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }
}
