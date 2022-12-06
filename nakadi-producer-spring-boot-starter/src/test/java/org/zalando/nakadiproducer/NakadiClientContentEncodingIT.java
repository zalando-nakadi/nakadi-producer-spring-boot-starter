package org.zalando.nakadiproducer;

import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zalando.fahrschein.http.api.ContentEncoding;
import org.zalando.fahrschein.http.api.RequestFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class NakadiClientContentEncodingIT extends BaseMockedExternalCommunicationIT {

    @Autowired
    private RequestFactory requestFactory;

    static {
        System.setProperty("nakadi-producer.encoding", "ZSTD");
    }

    @Test
    @SneakyThrows
    public void pickUpContentEncodingFromConfig() {
        final ContentEncoding contentEncoding = (ContentEncoding) FieldUtils.readField(requestFactory, "contentEncoding", true);
        assertThat(contentEncoding, is(ContentEncoding.ZSTD));
    }
}
