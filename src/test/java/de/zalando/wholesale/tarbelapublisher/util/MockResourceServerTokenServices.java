package de.zalando.wholesale.tarbelapublisher.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.zalando.stups.oauth2.spring.server.LaxAuthenticationExtractor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MockResourceServerTokenServices implements ResourceServerTokenServices {

    private final Map<String, String[]> tokensToScopes = new HashMap<>();
    private final List<String> roles = Lists.newArrayList();

    @Override
    public OAuth2Authentication loadAuthentication(final String accessToken) throws AuthenticationException,
            InvalidTokenException {
        String[] scopes = tokensToScopes.get(accessToken);
        if (scopes == null) {
            throw new InvalidTokenException("invalid token");
        }

        ImmutableMap<String, Object> tokenInfo = ImmutableMap.of(
                "uid", UUID.randomUUID().toString(),
                "scope", ImmutableList.copyOf(scopes)
        );

        return new LaxAuthenticationExtractor().extractAuthentication(tokenInfo, "dummyClientId",
                (String uid, String realm, String token) -> roles);
    }

    @Override
    public OAuth2AccessToken readAccessToken(final String s) {
        return null;
    }

    public String createTokenWithScopes(final String... scopes) {
        String newToken = UUID.randomUUID().toString();

        tokensToScopes.put(newToken, scopes);

        this.roles.clear();

        return newToken;
    }

    public String createTokenWithRolesAndScopes(final List<String> roles, final String... scopes) {
        String newToken = UUID.randomUUID().toString();

        tokensToScopes.put(newToken, scopes);

        this.roles.clear();
        this.roles.addAll(roles);

        return newToken;
    }
}
