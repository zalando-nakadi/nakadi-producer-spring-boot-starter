package de.zalando.wholesale.tarbelaproducer;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "tarbela")
public class TarbelaProperties {

    private String sinkId;

    private Integer snapshotBatchSize = 25;

}
