package org.zalando.nakadiproducer.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.zalando.nakadi_mock.NakadiMock;

import java.net.URL;


/**
 * An application context initializer which sets up a NakadiMock bean and registers the server URL as a property.
 */
class NakadiServerMockInitializer  implements ApplicationContextInitializer<ConfigurableApplicationContext>{

    private static final Logger LOG = LoggerFactory.getLogger(NakadiServerMockInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        // setup NakadiMock, inject URL into nakadi-producer

        NakadiMock mock = NakadiMock.make();
        context.getBeanFactory().registerSingleton("nakadiMock", mock);
        mock.start();
        URL url = mock.getRootUrl();

        LOG.info("started mock nakadi on {}", url);

        TestPropertyValues.of("nakadi-producer.nakadi-base-uri="+url).applyTo(context);
    }
}