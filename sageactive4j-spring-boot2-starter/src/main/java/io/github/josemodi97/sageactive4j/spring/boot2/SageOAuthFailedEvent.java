package io.github.josemodi97.sageactive4j.spring.boot2;

import io.github.josemodi97.sageactive4j.servlet.SageOAuthCallbackException;
import org.springframework.context.ApplicationEvent;

/** Published when a browser sign-in callback could not be completed. */
public class SageOAuthFailedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final SageOAuthCallbackException failure;

    public SageOAuthFailedEvent(Object source, SageOAuthCallbackException failure) {
        super(source);
        this.failure = failure;
    }

    public SageOAuthCallbackException getFailure() {
        return failure;
    }
}
