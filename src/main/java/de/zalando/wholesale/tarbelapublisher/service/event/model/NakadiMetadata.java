package de.zalando.wholesale.tarbelapublisher.service.event.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

import lombok.Data;

@Data
public class NakadiMetadata {

    @JsonProperty("eid")
    private String eid;

    @JsonProperty("occurred_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occuredAt;

}
