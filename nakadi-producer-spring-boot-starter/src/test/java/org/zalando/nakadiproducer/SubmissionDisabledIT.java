package org.zalando.nakadiproducer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.fahrschein.NakadiClient;
import org.zalando.fahrschein.http.api.RequestFactory;
import org.zalando.nakadiproducer.config.EmbeddedDataSourceConfig;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.eventlog.impl.EventLogRepository;
import org.zalando.nakadiproducer.transmission.NakadiPublishingClient;
import org.zalando.nakadiproducer.transmission.impl.EventTransmissionService;
import org.zalando.nakadiproducer.transmission.impl.EventTransmitter;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

// no "test" profile, as this would include the mock client.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"nakadi-producer.submission-enabled:false"},
        classes = { TestApplication.class, EmbeddedDataSourceConfig.class }
)
public class SubmissionDisabledIT  {

    @Autowired
    ApplicationContext context;

    @Test
    public void noNakadiBeans() {
        assertThat(context.getBeanProvider(NakadiClient.class).getIfAvailable(), nullValue());
        assertThat(context.getBeanProvider(NakadiPublishingClient.class).getIfAvailable(), nullValue());
        assertThat(context.getBeanProvider(StupsTokenComponent.class).getIfAvailable(), nullValue());
        assertThat(context.getBeanProvider(RequestFactory.class).getIfAvailable(), nullValue());
    }

    @Test
    public void noTransmissionBeans() {
        assertThat(context.getBeanProvider(EventTransmitter.class).getIfAvailable(), nullValue());
        assertThat(context.getBeanProvider(EventTransmissionService.class).getIfAvailable(), nullValue());
        assertThat(context.getBeanProvider(EventTransmissionScheduler.class).getIfAvailable(), nullValue());
    }

    @Test
    public void yesEventLogWriter() {
        assertThat(context.getBeanProvider(EventLogWriter.class).getIfAvailable(), notNullValue());
    }

    @Test
    public void yesRepository() {
        assertThat(context.getBeanProvider(EventLogRepository.class).getIfAvailable(), notNullValue());
    }

    @Test
    public void yesFlywayMigrator() {
        assertThat(context.getBeanProvider(FlywayMigrator.class).getIfAvailable(), notNullValue());
    }

}
