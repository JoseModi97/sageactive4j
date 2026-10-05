package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.Region;
import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.model.Organization;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Interactive setup: creates (or updates) a profile, makes it active,
 * optionally signs in and picks an organization. Every answer can also be
 * given as a flag, for scripted setup.
 */
@Command(name = "init", description = "Set up a profile: credentials, sign-in, organization.")
final class InitCommand extends CliCommand implements Callable<Integer> {

    static final String DEFAULT_REDIRECT_URI = "http://127.0.0.1:8765/callback";

    @Option(names = "--redirect-uri", description = "Loopback redirect URI registered for your app (default: "
            + DEFAULT_REDIRECT_URI + ")")
    String redirectUri;

    @Option(names = "--sandbox", negatable = true, description = "Mark the profile as a sandbox (allows 'invoice create')")
    Boolean sandbox;

    @Option(names = "--no-login", description = "Don't sign in now")
    boolean noLogin;

    @Option(names = "--no-browser", description = "Print the sign-in URL instead of opening a browser")
    boolean noBrowser;

    @Override
    public Integer call() {
        SageActive4jCli root = root();
        ConfigFile file = root.configFile();
        String name = ConfigFile.validProfileName(root.profile != null ? root.profile
                : prompt("Profile name", file.profiles().isEmpty() ? ConfigFile.DEFAULT_PROFILE : file.activeProfile()));

        out().println("Setting up profile '" + name + "' in " + file.path());
        String region = root.region != null ? root.region : prompt("Region (FR, ES, DE, PT)", or(file.get(name, "region"), "FR"));
        region = Region.parse(region).name();

        String subscriptionKey = root.subscriptionKey != null ? root.subscriptionKey : promptSecret(
                "Subscription key" + (file.get(name, "subscription-key") != null ? " (empty = keep current)" : ""));
        if (subscriptionKey == null) {
            subscriptionKey = file.get(name, "subscription-key");
        }
        if (subscriptionKey == null) {
            throw new CliException("A subscription key is required (Sage Developer Center > your app > Subscription keys).");
        }

        String clientId = root.clientId != null ? root.clientId : prompt("OAuth client id", file.get(name, "client-id"));
        String clientSecret = root.clientSecret != null ? root.clientSecret
                : promptSecret("OAuth client secret (empty = none/keep; prefer SAGEACTIVE4J_CLIENT_SECRET)");
        if (clientSecret == null) {
            clientSecret = file.get(name, "client-secret");
        }
        String redirect = redirectUri != null ? redirectUri
                : prompt("Redirect URI (must be registered for your app)", or(file.get(name, "redirect-uri"), DEFAULT_REDIRECT_URI));
        boolean isSandbox = sandbox != null ? sandbox
                : confirm("Is this a sandbox (test) account?", "true".equals(file.get(name, "sandbox")));

        file.set(name, "region", region);
        file.set(name, "subscription-key", subscriptionKey);
        file.set(name, "client-id", clientId);
        file.set(name, "client-secret", clientSecret);
        file.set(name, "redirect-uri", redirect);
        file.set(name, "sandbox", isSandbox ? "true" : "false");
        file.setActiveProfile(name);
        file.save();
        out().println("Saved profile '" + name + "' (now active) to " + file.path());
        if (clientSecret != null) {
            out().println("Note: the client secret is stored in that file, readable only by you. "
                    + "To keep it out of the file, remove it and set SAGEACTIVE4J_CLIENT_SECRET instead.");
        }

        if (noLogin || clientId == null || !confirm("Sign in now?", true)) {
            out().println("Next: sageactive4j login, then sageactive4j org");
            return 0;
        }
        root.profile = name;
        LoopbackLogin.signIn(root, null, LoopbackLogin.DEFAULT_TIMEOUT_SECONDS, !noBrowser);

        try (SageActive4jClient client = root.client()) {
            List<Organization> usable = client.organizations().listUsable();
            if (usable.isEmpty()) {
                out().println("No usable organization found for this user (they must be READY and onboarded).");
                return 0;
            }
            for (int i = 0; i < usable.size(); i++) {
                Organization o = usable.get(i);
                out().println("  " + (i + 1) + ") " + o.getSocialName() + " [" + o.getLegislationCode() + "] " + o.getId());
            }
            String choice = prompt("Organization to use (number)", "1");
            int index;
            try {
                index = Integer.parseInt(choice.trim()) - 1;
            } catch (NumberFormatException e) {
                index = -1;
            }
            if (index < 0 || index >= usable.size()) {
                throw new CliException("No organization number " + choice + "; pick one later with: sageactive4j org set <id>");
            }
            ConfigFile updated = root.configFile();
            updated.set(name, "organization-id", usable.get(index).getId());
            updated.save();
            out().println("Using organization " + usable.get(index).getSocialName() + ". Check everything with: sageactive4j test");
        }
        return 0;
    }

    private static String or(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
