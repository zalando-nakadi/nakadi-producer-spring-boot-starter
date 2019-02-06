package org.zalando.nakadiproducer.transmission.impl;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class NakadiMetadata {

    @JsonProperty("eid")
    private String eid;

    @JsonProperty("occurred_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occuredAt;

    @JsonProperty("flow_id")
    private String flowId;

    @JsonProperty("span_ctx")
    private Map<String, String> spanContext;

}
