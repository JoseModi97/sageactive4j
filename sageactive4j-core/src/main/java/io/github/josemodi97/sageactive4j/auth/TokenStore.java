package io.github.josemodi97.sageactive4j.auth;

/**
 * Where a client keeps its tokens. SBC Auth rotates the refresh token on
 * refresh, so the newest token set has to be saved somewhere that outlives
 * the process - otherwise a restart falls back to a refresh token that has
 * already been used.
 *
 * <p>Built in: {@link InMemoryTokenStore} (the default) and
 * {@link FileTokenStore}. For a multi-instance service, implement this
 * against your database or cache. Implementations must be thread-safe;
 * the client never calls them concurrently for the same token.
 */
public interface TokenStore {

    /** The saved token set, or {@code null} if there is none. */
    SageToken load();

    /** Replaces the saved token set. */
    void save(SageToken token);

    /** Removes the saved token set (e.g. after sign-out). */
    void clear();
}
