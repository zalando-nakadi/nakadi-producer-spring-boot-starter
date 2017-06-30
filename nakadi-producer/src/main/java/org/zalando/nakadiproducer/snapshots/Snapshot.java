package org.zalando.nakadiproducer.snapshots;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Snapshot {
    private Object id;
    private String eventType;
    private String dataType;
    private Object data;
}
