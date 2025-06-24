package org.zalando.nakadiproducer;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.nakadiproducer.config.EmbeddedDataSourceConfig;


@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
            "zalando.team.id:alpha-local-testing",
            "nakadi-producer.scheduled-transmission-enabled:false",
            "nakadi-producer.lock-size:100"
        },
        classes = { TestApplication.class, EmbeddedDataSourceConfig.class }
)
public abstract class BaseMockedExternalCommunicationIT {

}
