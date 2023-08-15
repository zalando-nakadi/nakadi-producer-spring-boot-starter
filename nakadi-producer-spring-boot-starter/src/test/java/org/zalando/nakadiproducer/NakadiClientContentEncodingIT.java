package org.zalando.nakadiproducer;

import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.zalando.fahrschein.http.api.ContentEncoding;
import org.zalando.fahrschein.http.api.RequestFactory;
import org.zalando.nakadiproducer.config.EmbeddedDataSourceConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

// this class has no @ActiveProfiles("test"), so it doesn't use the MockNakadiClient.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "nakadi-producer.scheduled-transmission-enabled:false",
                // as we are not defining a mock nakadi client, we need to provide these properties:
                "nakadi-producer.encoding:ZSTD",
                "nakadi-producer.nakadi-base-uri:http://nakadi.example.com/",
        },
        classes = { TestApplication.class, EmbeddedDataSourceConfig.class }
)
public class NakadiClientContentEncodingIT {

    // Avoid errors in the logs from the AccessTokenRefresher. As we are not actually submitting
    // to Nakadi, this will never be used.
    @MockBean
    private AccessTokenProvider tokenProvider;

    @Autowired
    private RequestFactory requestFactory;

    @Test
    @SneakyThrows
    public void pickUpContentEncodingFromConfig() {
        final ContentEncoding contentEncoding =
                (ContentEncoding) FieldUtils.readField(requestFactory, "contentEncoding", true);
        assertThat(contentEncoding, is(ContentEncoding.ZSTD));
    }
}
