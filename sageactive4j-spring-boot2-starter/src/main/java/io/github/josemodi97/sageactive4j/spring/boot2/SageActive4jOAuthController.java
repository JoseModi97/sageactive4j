package io.github.josemodi97.sageactive4j.spring.boot2;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.servlet.SageOAuthCallbackHandler;
import io.github.josemodi97.sageactive4j.servlet.SageOAuthFlow;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Browser sign-in endpoints, enabled with {@code sageactive4j.oauth.enabled=true}:
 * the login path redirects to SBC Auth, the callback path completes sign-in,
 * saves the tokens and publishes {@link SageTokenAcquiredEvent} (or
 * {@link SageOAuthFailedEvent}).
 *
 * <p><strong>Protect the login path</strong> (e.g. with Spring Security):
 * whoever completes sign-in sets the credentials every Sage Active call of
 * this app then uses. The callback path itself is safe to leave open - it
 * only accepts the single-use state issued to the browser session that
 * started sign-in.
 */
@Controller
public class SageActive4jOAuthController {

    private static final Log LOG = LogFactory.getLog(SageActive4jOAuthController.class);

    private final SageOAuthFlow flow;
    private final SageOAuthCallbackHandler handler;

    public SageActive4jOAuthController(SageActive4jClient client, SageActive4jProperties properties,
                                       ApplicationEventPublisher events) {
        SageActive4jProperties.OAuth oauth = properties.getOauth();
        String redirectUri = oauth.getRedirectUri() != null ? oauth.getRedirectUri() : properties.getRedirectUri();
        if (redirectUri == null || redirectUri.trim().isEmpty()) {
            throw new IllegalStateException("sageactive4j.oauth.enabled=true needs a redirect URI: set "
                    + "sageactive4j.redirect-uri (or sageactive4j.oauth.redirect-uri) to the callback URL "
                    + "registered for your app, ending in " + oauth.getCallbackPath());
        }
        if (properties.getClientId() == null || properties.getClientId().trim().isEmpty()) {
            throw new IllegalStateException("sageactive4j.oauth.enabled=true needs sageactive4j.client-id");
        }
        this.flow = new SageOAuthFlow(client.auth(), redirectUri, oauth.isPkce(), oauth.getMaxAge(), Clock.systemUTC());
        final String successUrl = oauth.getSuccessUrl();
        this.handler = new SageOAuthCallbackHandler(flow,
                (request, response, token) -> {
                    events.publishEvent(new SageTokenAcquiredEvent(this, token));
                    if (successUrl != null && !successUrl.trim().isEmpty()) {
                        response.sendRedirect(successUrl);
                    }
                },
                (request, response, failure) -> {
                    LOG.warn("Sage Active sign-in failed: " + failure.getMessage());
                    events.publishEvent(new SageOAuthFailedEvent(this, failure));
                });
        LOG.warn("Sage Active sign-in endpoints are enabled (" + oauth.getLoginPath() + ", " + oauth.getCallbackPath()
                + "): make sure only administrators can reach " + oauth.getLoginPath());
    }

    @GetMapping("${sageactive4j.oauth.login-path:/sage/oauth/login}")
    public void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        flow.redirect(request, response);
    }

    @GetMapping("${sageactive4j.oauth.callback-path:/sage/oauth/callback}")
    public void callback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handler.handle(request, response);
    }
}
