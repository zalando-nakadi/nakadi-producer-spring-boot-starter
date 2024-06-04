package org.zalando.nakadiproducer.eventlog.impl;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
class DataChangeEventEnvelope {
    @JsonProperty("data_op")
    private String dataOp;

    @JsonProperty("data_type")
    private String dataType;

    @JsonProperty("data")
    private Object data;
}
