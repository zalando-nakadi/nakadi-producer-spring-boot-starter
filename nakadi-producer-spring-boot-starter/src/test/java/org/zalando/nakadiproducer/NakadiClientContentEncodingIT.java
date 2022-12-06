package org.zalando.nakadiproducer;

import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.fahrschein.http.api.ContentEncoding;
import org.zalando.fahrschein.http.api.RequestFactory;
import org.zalando.nakadiproducer.config.EmbeddedDataSourceConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = { "zalando.team.id:alpha-local-testing", "nakadi-producer.scheduled-transmission-enabled:false", "nakadi-producer.encoding:ZSTD" },
        classes = { TestApplication.class, EmbeddedDataSourceConfig.class }
)
public class NakadiClientContentEncodingIT {

    @Autowired
    private RequestFactory requestFactory;

    @Test
    @SneakyThrows
    public void pickUpContentEncodingFromConfig() {
        final ContentEncoding contentEncoding = (ContentEncoding) FieldUtils.readField(requestFactory, "contentEncoding", true);
        assertThat(contentEncoding, is(ContentEncoding.ZSTD));
    }
}
