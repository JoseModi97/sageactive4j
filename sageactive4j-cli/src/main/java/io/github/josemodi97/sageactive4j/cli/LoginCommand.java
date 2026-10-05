package io.github.josemodi97.sageactive4j.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Signs in through the browser and stores the profile's tokens. */
@Command(name = "login", description = "Sign in to Sage Active in your browser and store the tokens for this profile.")
final class LoginCommand extends CliCommand implements Callable<Integer> {

    @Option(names = "--no-browser", description = "Print the sign-in URL instead of opening a browser")
    boolean noBrowser;

    @Option(names = "--timeout", description = "Seconds to wait for the sign-in (default: ${DEFAULT-VALUE})",
            defaultValue = "" + LoopbackLogin.DEFAULT_TIMEOUT_SECONDS)
    int timeoutSeconds;

    @Option(names = "--port", hidden = true, description = "Listen on this port instead of the redirect URI's")
    Integer port;

    @Override
    public Integer call() {
        LoopbackLogin.signIn(root(), port, timeoutSeconds, !noBrowser);
        return 0;
    }
}
