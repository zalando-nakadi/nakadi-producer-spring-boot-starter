package org.zalando.nakadiproducer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.Connection;

import org.flywaydb.core.api.callback.FlywayCallback;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.zalando.nakadiproducer.config.EmbeddedDataSourceConfig;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = { "zalando.team.id:alpha-local-testing", "nakadi-producer.scheduled-transmission-enabled:false", "spring.flyway.enabled:false"},
        classes = { TestApplication.class, EmbeddedDataSourceConfig.class }
)
public class NonNakadiProducerFlywayCallbackIT {

    @MockBean
    private FlywayCallback flywayCallback;

    @Test
    public void flywayCallbacksFromOurHostApplicationAreNotUsedByUs() {
        verify(flywayCallback, never()).beforeValidate(any(Connection.class));
    }

    @Test
    public void ourOwnFlywayConfigurationStillWorksFineWhenSpringsFlywayAutoconfigIsDisabled() {
        // Yes, this is redundant to the other test in here.
        // We consider it important to document the requirement, so it is here nonetheless.
        // The test setup done by the class annotations does just enough to test it
    }
}
