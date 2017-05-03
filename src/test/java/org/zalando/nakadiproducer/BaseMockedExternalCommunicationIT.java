package org.zalando.nakadiproducer;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.zalando.nakadiproducer.config.EmbeddedDataSourceConfig;


@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = { "zalando.team.id:alpha-local-testing", "nakadi-producer.scheduled-transmission-enabled:false" },
        classes = { TestApplication.class, EmbeddedDataSourceConfig.class }
)
public abstract class BaseMockedExternalCommunicationIT {

}
