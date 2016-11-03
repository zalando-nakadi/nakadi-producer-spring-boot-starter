package de.zalando.wholesale.tarbelapublisher;

import com.jayway.restassured.specification.RequestSpecification;

import de.zalando.wholesale.tarbelapublisher.config.AuthServerMockConfig;
import de.zalando.wholesale.tarbelapublisher.config.EmbeddedDataSourceConfig;
import de.zalando.wholesale.tarbelapublisher.util.MockResourceServerTokenServices;

import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;

import static com.jayway.restassured.RestAssured.given;


@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = { "server.port:50203", "management.port:0", "zalando.team.id:alpha-local-testing" },
        classes = { TestApplication.class, EmbeddedDataSourceConfig.class, AuthServerMockConfig.class }
)
public abstract class BaseMockedExternalCommunicationIT {
    protected static final String UID_SCOPE = "uid";
    protected static final String READ_SCOPE = "tarbela-publisher.read";
    protected static final String EVENT_LOG_WRITE_SCOPE = "tarbela-publisher.event_log_write";

    private static final String[] ALL_KNOWN_SCOPES = {
        UID_SCOPE, READ_SCOPE, EVENT_LOG_WRITE_SCOPE
    };

    @Autowired
    private MockResourceServerTokenServices mockResourceServerTokenServices;

    @Value("${local.server.port}")
    int httpPort;

    protected void fakeAuthForAuditing(final String userName) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new User(userName, "abc", new ArrayList<>()), null));
    }

    @After
    public void resetAuthToNone() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    protected RequestSpecification anAuthorizedRequest() {
        return anAuthenticatedRequestWithScopes(ALL_KNOWN_SCOPES);
    }

    protected RequestSpecification anAuthenticatedRequestWithScopes(final String... scopes) {
        String oauth2AccessToken = mockResourceServerTokenServices.createTokenWithScopes(scopes);
        return aHttpsRequest().header("Authorization", "Bearer " + oauth2AccessToken);
    }

    protected RequestSpecification aHttpsRequest() {
        return given().relaxedHTTPSValidation().baseUri("http://localhost:" + httpPort);
    }
}
