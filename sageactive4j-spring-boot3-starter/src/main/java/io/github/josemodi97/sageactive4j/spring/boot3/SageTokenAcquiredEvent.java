package io.github.josemodi97.sageactive4j.spring.boot3;

import io.github.josemodi97.sageactive4j.auth.SageToken;
import org.springframework.context.ApplicationEvent;

/**
 * Published after a browser sign-in completed and the tokens were saved to
 * the {@code TokenStore}. Its {@code toString()} masks the tokens.
 */
public class SageTokenAcquiredEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final transient SageToken token;

    public SageTokenAcquiredEvent(Object source, SageToken token) {
        super(source);
        this.token = token;
    }

    public SageToken getToken() {
        return token;
    }
}
