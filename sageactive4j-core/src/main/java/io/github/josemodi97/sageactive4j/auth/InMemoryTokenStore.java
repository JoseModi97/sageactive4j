package io.github.josemodi97.sageactive4j.auth;

/** Keeps tokens in memory only; they're lost when the process exits. The default store. */
public final class InMemoryTokenStore implements TokenStore {

    private volatile SageToken token;

    @Override
    public SageToken load() {
        return token;
    }

    @Override
    public void save(SageToken token) {
        this.token = token;
    }

    @Override
    public void clear() {
        this.token = null;
    }
}
