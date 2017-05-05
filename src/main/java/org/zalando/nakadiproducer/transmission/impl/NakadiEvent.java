package org.zalando.nakadiproducer.transmission.impl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;

import lombok.Data;

@Data
public class NakadiEvent {

    @JsonProperty("data_type")
    private String dataType;

    @JsonProperty("data_op")
    private String dataOperation;

    @JsonProperty("data")
    private HashMap<String, Object> data;

    @JsonProperty("metadata")
    private NakadiMetadata metadata;

}
