package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.SageActive4jConfig;
import io.github.josemodi97.sageactive4j.auth.SageAuthClient;
import io.github.josemodi97.sageactive4j.exception.SageActive4jException;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** Revokes the profile's refresh token at SBC Auth and deletes the token file. */
@Command(name = "logout", description = "Revoke this profile's tokens and delete them locally.")
final class LogoutCommand extends CliCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        SageActive4jConfig config = root().config();
        if (config.getTokenStore() == null) {
            out().println("Nothing to sign out of: an access token was given on the command line or environment.");
            return 0;
        }
        if (config.getTokenStore().load() == null) {
            out().println("Not signed in.");
            return 0;
        }
        try {
            new SageAuthClient(config).revoke();
            out().println("Signed out: tokens revoked and deleted.");
        } catch (SageActive4jException e) {
            // Still remove them locally: the user asked to sign out.
            config.getTokenStore().clear();
            root().context.err.println("Warning: revoking at SBC Auth failed (" + e.getMessage()
                    + "); the local tokens were deleted anyway.");
        }
        return 0;
    }
}
