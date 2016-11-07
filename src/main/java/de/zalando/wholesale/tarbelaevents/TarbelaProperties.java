package de.zalando.wholesale.tarbelaevents;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "tarbela")
public class TarbelaProperties {

    private String eventType;

    private String dataType;

    private String sinkId;

    private Integer snapshotBatchSize = 25;

}
