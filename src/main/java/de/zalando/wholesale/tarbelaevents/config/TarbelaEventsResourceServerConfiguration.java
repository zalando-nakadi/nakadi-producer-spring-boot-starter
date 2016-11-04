package de.zalando.wholesale.tarbelaevents.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableResourceServer
public class TarbelaEventsResourceServerConfiguration extends ResourceServerConfigurerAdapter {

    @Value("${spring.oauth2.application.scope.read.tarbela_event_log}")
    private String oauthScopeRead;

    @Value("${spring.oauth2.application.scope.write.tarbela_event_log}")
    private String oauthScopeWriteEventLog;

    @Value("${server.port}")
    private int apiPort;

    /**
     * Configure scopes for specific controller/httpmethods/roles here.
     */
    @Override
    public void configure(final HttpSecurity http) throws Exception {
        http
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.NEVER)
            .and()
            .authorizeRequests()
                .requestMatchers(forPortAndPath(apiPort, HttpMethod.GET, "/events/**")).access(oauthScopeRead)
                .requestMatchers(forPortAndPath(apiPort, HttpMethod.PATCH, "/events/**")).access(oauthScopeWriteEventLog)
                .requestMatchers(forPortAndPath(apiPort, HttpMethod.POST, "/events/snapshots/**")).access(oauthScopeWriteEventLog);
    }

    /**
     * Creates a request matcher which only matches requests for a specific local port, path and request method (using
     * an {@link AntPathRequestMatcher} for the path part).
     *
     * @param   port         the port to match
     * @param   pathPattern  the pattern for the path.
     * @param   method       the HttpMethod to match. Requests for other methods will not be matched.
     *
     * @return  the new request matcher.
     */
    private RequestMatcher forPortAndPath(final int port, @Nonnull final HttpMethod method,
            @Nonnull final String pathPattern) {
        return new AndRequestMatcher(forPort(port), new AntPathRequestMatcher(pathPattern, method.name()));
    }

    /**
     * A request matcher which matches just a port.
     *
     * @param   port  the port to match.
     *
     * @return  the new matcher.
     */
    private RequestMatcher forPort(final int port) {
        return (HttpServletRequest request) -> port == request.getLocalPort();
    }

}
