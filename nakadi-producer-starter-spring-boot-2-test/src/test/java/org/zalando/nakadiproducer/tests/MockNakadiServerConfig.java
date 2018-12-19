package org.zalando.nakadiproducer.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.zalando.nakadi_mock.NakadiMock;

import java.net.URL;

@Configuration
public class MockNakadiServerConfig {

    static class MockPropertyInitializer  implements ApplicationContextInitializer<ConfigurableApplicationContext>{

        private static final Logger LOG = LoggerFactory.getLogger(MockPropertyInitializer.class);

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            // TODO: setup NakadiMock, inject URL into nakadi-producer

            NakadiMock mock = NakadiMock.make();
            context.getBeanFactory().registerSingleton("nakadiMock", mock);
            mock.start();
            URL url = mock.getRootUrl();

            LOG.info("started mock nakadi on {}", url);

            TestPropertyValues.of("nakadi-producer.nakadi-base-url="+url).applyTo(context);
        }
    }
}
