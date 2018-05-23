package org.zalando.nakadiproducer;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.zalando.nakadiproducer.config.EmbeddedDataSourceConfig;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;
import org.zalando.nakadiproducer.util.example_app.ExampleApp;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * This tests different auto configuration options, each in its own application
 * context.
 */
@Slf4j
public class AutoConfigurationTest {

    private AnnotationConfigApplicationContext context;
    private MockEnvironment environment;

    @Before
    public void setUp() throws Exception {
        context = new AnnotationConfigApplicationContext();
        environment = new MockEnvironment();
        context.setEnvironment(environment);
    }

    @After
    public void tearDown() {
        context.close();
    }

    @Test
    public void testFailureWithoutAnything() {
        // Nothing configured – no data source, no Nakadi URL, no token stuff.
        context.register(ExampleApp.class);
        context.register(NakadiProducerAutoConfiguration.class);
        try {
            context.refresh();
            fail("This should not work!");
        } catch (UnsatisfiedDependencyException ex) {
            // expected – possibly verify that it is a useful error message?
            assertThat(ex.getMessage(),
                    containsString("org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate"));
            log.info("Expected exception: ", ex);
        }
    }

    private void verifyEventWriting(EventLogWriter writer) throws InterruptedException {
        MockPayload payload = Fixture.mockPayload(1, "BLA");
        writer.fireBusinessEvent("type", payload);
        // TODO: remove this, find a way to check that it actually worked.
        Thread.sleep(10000);
    }

    @Test
    public void testFailureWithOnlyDatasource() throws InterruptedException {
        // Now we have a database, but still no token configured.
        context.register(ExampleApp.class);
        context.register(NakadiProducerAutoConfiguration.class);
        context.register(EmbeddedDataSourceConfig.class);
        try {
            context.refresh();
            fail("This should not work!");
        } catch (UnsatisfiedDependencyException ex) {
            // expected – possibly verify that it is a useful error message?
            assertThat(ex.getMessage(),
                    containsString("org.zalando.nakadiproducer.AccessTokenProvider"));
            log.info("Expected exception: ", ex);
        }
    }


    @Test
    public void testHappyWithDatasourceAndProperties() throws InterruptedException {
        // Now we have a database, but still no token configured.
        context.register(ExampleApp.class);
        context.register(NakadiProducerAutoConfiguration.class);
        context.register(EmbeddedDataSourceConfig.class);
        environment.setProperty("nakadi-producer.access-token-uri", "https://token.auth.example.org/oauth2/access_token");
        environment.setProperty("nakadi-producer.access-token-scopes", "uid");
        environment.setProperty("nakadi-producer.nakadi-base-uri", "https://nakadi.example.org");
        try {
            context.refresh();
            // TODO: now try to actually write something
            fail("This should not work!");
        } catch (UnsatisfiedDependencyException ex) {
            // expected – possibly verify that it is a useful error message?
            assertThat(ex.getMessage(),
                    containsString("org.zalando.nakadiproducer.AccessTokenProvider"));
            log.info("Expected exception: ", ex);
        }
    }

}
