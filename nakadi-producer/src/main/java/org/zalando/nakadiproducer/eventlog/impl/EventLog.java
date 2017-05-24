package org.zalando.nakadiproducer.eventlog.impl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class EventLog {

    private Integer id;
    private String eventType;
    private String eventBodyData;
    private String flowId;
    private Instant created;
    private Instant lastModified;
    private String lockedBy;
    private Instant lockedUntil;

}
