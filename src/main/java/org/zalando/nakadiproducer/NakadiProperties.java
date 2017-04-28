package org.zalando.nakadiproducer;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "nakadi-producer")
public class NakadiProperties {
    private Integer snapshotBatchSize = 25;
}
