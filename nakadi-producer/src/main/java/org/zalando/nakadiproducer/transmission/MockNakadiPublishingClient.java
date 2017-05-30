package org.zalando.nakadiproducer.transmission;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

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
    public synchronized void publish(String eventType, List<?> nakadiEvents) {
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
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static ObjectMapper createDefaultObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModules(new Jdk8Module(), new ParameterNamesModule(), new JavaTimeModule());
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return objectMapper;
    }
}
