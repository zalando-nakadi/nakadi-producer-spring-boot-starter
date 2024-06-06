package org.zalando.nakadiproducer.eventlog.impl;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class EventLog {

    public EventLog(Integer id, String eventType, String eventBodyData, String flowId,
                    Instant created,
                    Instant lastModified, String lockedBy, Instant lockedUntil,
                    String compactionKey) {
        this.id = id;
        this.eventType = eventType;
        this.eventBodyData = eventBodyData;
        this.flowId = flowId;
        this.created = created;
        this.lastModified = lastModified;
        this.lockedBy = lockedBy;
        this.lockedUntil = lockedUntil;
        this.compactionKey = compactionKey;
    }

    private Integer id;
    private String eventType;
    private String eventBodyData;
    private String flowId;
    private Instant created;
    private Instant lastModified;
    private String lockedBy;
    private Instant lockedUntil;
    private String compactionKey;
    private UUID eid;

    /**
     * This is only needed for backward compatibility.
     *
     * <p>For instance 213 will be converted to "00000000-0000-0000-0000-0000000000d5"</p>
     */
    public UUID getEid() {
        if (eid == null) {
            return new UUID(0, id);
        }

        return eid;
    }
}
