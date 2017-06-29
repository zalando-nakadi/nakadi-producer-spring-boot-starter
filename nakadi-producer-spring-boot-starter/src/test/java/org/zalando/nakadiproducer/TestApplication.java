package org.zalando.nakadiproducer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableNakadiProducer
public class TestApplication {

    public static void main(final String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
