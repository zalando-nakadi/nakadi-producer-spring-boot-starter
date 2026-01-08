package org.zalando.nakadiproducer.transmission.impl;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import nakadi.EventMetadata;
import nakadi.WithEventMetadata;

import java.util.HashMap;

@Data
public class NakadiEvent implements WithEventMetadata {
    @JsonIgnore
    private HashMap<String, Object> data;

    @JsonProperty("metadata")
    private EventMetadata metadata;

    // "any getter" needed for serialization - we use it to extract the properties of the data object and put them in
    // the top level of the serialized JSON, to conform to Nakadi's business event structure
    @JsonAnyGetter
    public HashMap<String,Object> any() {
        return data;
    }

    @Override
    public EventMetadata metadata() {
        return metadata;
    }
}
