package org.zalando.nakadiproducer.transmission.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import nakadi.EventMetadata;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class EventMetadataSerializer extends JsonSerializer<EventMetadata> {
    @Override
    public void serialize(EventMetadata value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        if (value.eid() != null) {
            gen.writeStringField("eid", value.eid());
        }
        if (value.occurredAt() != null) {
            gen.writeStringField("occurred_at", value.occurredAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        if (value.flowId() != null) {
            gen.writeStringField("flow_id", value.flowId());
        }
        if (value.partition() != null) {
            gen.writeStringField("partition", value.partition());
        }
        if (value.partitionCompactionKey() != null) {
            gen.writeStringField("partition_compaction_key", value.partitionCompactionKey());
        }
        if (value.spanCtx() != null) {
            gen.writeObjectField("span_ctx", value.spanCtx());
        }
        gen.writeEndObject();
    }
}
