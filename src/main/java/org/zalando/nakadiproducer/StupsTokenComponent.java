package org.zalando.nakadiproducer;

import java.net.URI;
import java.util.Collection;

import org.zalando.stups.tokens.AccessTokens;
import org.zalando.stups.tokens.Tokens;

class StupsTokenComponent implements AccessTokenProvider {

    private static final String TOKEN_ID = "nakadi";
    private AccessTokens accessTokens;

    public StupsTokenComponent(URI accessTokenUri, Collection<String> accessTokenScopes) {
        accessTokens = Tokens.createAccessTokensWithUri(URI.create("accessTokenUri"))
                             .manageToken(TOKEN_ID)
                             .addScopesTypeSafe(accessTokenScopes)
                             .done()
                             .start();
    }

    public void stop() {

    }

    @Override
    public String getAccessToken() {
        return accessTokens.get(TOKEN_ID);
    }
}
