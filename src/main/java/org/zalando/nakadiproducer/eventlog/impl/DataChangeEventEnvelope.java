package org.zalando.nakadiproducer.eventlog.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@AllArgsConstructor
class DataChangeEventEnvelope {
    @JsonProperty("data_op")
    String dataOp;

    @JsonProperty("data_type")
    String dataType;

    @JsonProperty("data")
    Object data;
}
