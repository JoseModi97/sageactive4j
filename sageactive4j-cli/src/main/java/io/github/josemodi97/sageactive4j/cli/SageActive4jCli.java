package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.Region;
import io.github.josemodi97.sageactive4j.SageActive4j;
import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.SageActive4jConfig;
import io.github.josemodi97.sageactive4j.auth.FileTokenStore;
import io.github.josemodi97.sageactive4j.exception.SageActive4jException;
import io.github.josemodi97.sageactive4j.exception.SageActive4jGraphQLException;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

/**
 * {@code sageactive4j}: set up, sign in to and explore Sage Active from the
 * terminal. Settings resolve flag, then {@code SAGEACTIVE4J_*} environment
 * variable, then the active profile in {@code ~/.sageactive4j/config.properties}.
 */
@Command(
        name = "sageactive4j",
        mixinStandardHelpOptions = true,
        versionProvider = SageActive4jCli.Version.class,
        description = {"Command-line companion to the sageactive4j SDK for the Sage Active Public API V2.",
            "Settings: --flag > SAGEACTIVE4J_* environment variable > profile (see 'env')."},
        subcommands = {InitCommand.class, LoginCommand.class, LogoutCommand.class, EnvCommand.class,
            TestCommand.class, OrgCommand.class, QueryCommand.class, InvoiceCommand.class,
            CommandLine.HelpCommand.class})
public final class SageActive4jCli implements Runnable {

    final CliContext context;

    @Option(names = "--profile", scope = ScopeType.INHERIT, description = "Profile to use (or SAGEACTIVE4J_PROFILE; default: the active one)")
    String profile;

    @Option(names = "--home", scope = ScopeType.INHERIT, description = "Settings directory (or SAGEACTIVE4J_HOME; default: ~/.sageactive4j)")
    String home;

    @Option(names = "--region", scope = ScopeType.INHERIT, description = "FR, ES, DE or PT")
    String region;

    @Option(names = "--subscription-key", scope = ScopeType.INHERIT, description = "App subscription key")
    String subscriptionKey;

    @Option(names = "--organization", scope = ScopeType.INHERIT, description = "Organization id (X-OrganizationId)")
    String organization;

    @Option(names = "--client-id", scope = ScopeType.INHERIT, description = "OAuth client id")
    String clientId;

    @Option(names = "--client-secret", scope = ScopeType.INHERIT, description = "OAuth client secret (prefer SAGEACTIVE4J_CLIENT_SECRET)")
    String clientSecret;

    @Option(names = "--access-token", scope = ScopeType.INHERIT, description = "Use this access token instead of signing in")
    String accessToken;

    @Option(names = "--base-url", scope = ScopeType.INHERIT, hidden = true)
    String baseUrl;

    @Option(names = "--auth-url", scope = ScopeType.INHERIT, hidden = true)
    String authUrl;

    @Option(names = "--token-url", scope = ScopeType.INHERIT, hidden = true)
    String tokenUrl;

    @Option(names = "--verbose", scope = ScopeType.INHERIT, description = "Show stack traces on errors")
    boolean verbose;

    SageActive4jCli(CliContext context) {
        this.context = context;
    }

    public static void main(String[] args) {
        System.exit(run(CliContext.system(), args));
    }

    static int run(CliContext context, String... args) {
        return commandLine(context).execute(args);
    }

    static CommandLine commandLine(CliContext context) {
        final SageActive4jCli root = new SageActive4jCli(context);
        CommandLine cmd = new CommandLine(root);
        cmd.setOut(new java.io.PrintWriter(context.out, true));
        cmd.setErr(new java.io.PrintWriter(context.err, true));
        cmd.setExecutionExceptionHandler((e, commandLine, parseResult) -> {
            if (root.verbose) {
                e.printStackTrace(context.err);
            }
            context.err.println("Error: " + describe(e));
            return 1;
        });
        return cmd;
    }

    static String describe(Exception e) {
        if (e instanceof SageActive4jGraphQLException) {
            return ((SageActive4jGraphQLException) e).getErrors().get(0).toString();
        }
        if (e instanceof SageActive4jException || e instanceof IllegalArgumentException
                || e instanceof IllegalStateException || e instanceof CliException) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }

    @Override
    public void run() {
        new CommandLine(this).usage(context.out);
    }

    // ---------------------------------------------------------------- shared by subcommands

    ConfigFile configFile() {
        return ConfigFile.load(context.home(home));
    }

    /** {@code --profile}, else {@code SAGEACTIVE4J_PROFILE}, else the file's active profile. */
    String profileName(ConfigFile file) {
        if (profile != null) {
            return ConfigFile.validProfileName(profile);
        }
        String fromEnv = context.env.get("SAGEACTIVE4J_PROFILE");
        if (fromEnv != null && !fromEnv.trim().isEmpty()) {
            return ConfigFile.validProfileName(fromEnv.trim());
        }
        return file.activeProfile();
    }

    /** The effective setting: flag, then environment, then profile. */
    String setting(ConfigFile file, String profileName, String flag, String envVar, String profileKey) {
        if (flag != null && !flag.trim().isEmpty()) {
            return flag.trim();
        }
        String fromEnv = context.env.get(envVar);
        if (fromEnv != null && !fromEnv.trim().isEmpty()) {
            return fromEnv.trim();
        }
        return file.get(profileName, profileKey);
    }

    SageActive4jConfig config() {
        ConfigFile file = configFile();
        String name = profileName(file);
        Map<String, String> env = context.env;
        String regionValue = setting(file, name, region, "SAGEACTIVE4J_REGION", "region");
        String token = setting(file, name, accessToken, "SAGEACTIVE4J_ACCESS_TOKEN", "access-token");
        SageActive4jConfig.Builder builder = SageActive4jConfig.builder()
                .region(regionValue == null ? Region.FR : Region.parse(regionValue))
                .subscriptionKey(setting(file, name, subscriptionKey, "SAGEACTIVE4J_SUBSCRIPTION_KEY", "subscription-key"))
                .organizationId(setting(file, name, organization, "SAGEACTIVE4J_ORGANIZATION_ID", "organization-id"))
                .clientId(setting(file, name, clientId, "SAGEACTIVE4J_CLIENT_ID", "client-id"))
                .clientSecret(setting(file, name, clientSecret, "SAGEACTIVE4J_CLIENT_SECRET", "client-secret"))
                .redirectUri(setting(file, name, null, "SAGEACTIVE4J_REDIRECT_URI", "redirect-uri"))
                .baseUrl(setting(file, name, baseUrl, "SAGEACTIVE4J_BASE_URL", "base-url"))
                .authUrl(setting(file, name, authUrl, "SAGEACTIVE4J_AUTH_URL", "auth-url"))
                .tokenUrl(setting(file, name, tokenUrl, "SAGEACTIVE4J_TOKEN_URL", "token-url"))
                .revocationUrl(setting(file, name, null, "SAGEACTIVE4J_REVOCATION_URL", "revocation-url"))
                .accessToken(token)
                .refreshToken(env.get("SAGEACTIVE4J_REFRESH_TOKEN"));
        if (token == null) {
            // Signed-in tokens live per profile; an explicit access token bypasses them.
            builder.tokenStore(new FileTokenStore(file.tokenFile(name)));
        }
        return builder.build();
    }

    SageActive4jClient client() {
        SageActive4jConfig config = config();
        if (config.getSubscriptionKey() == null) {
            throw new CliException("No subscription key. Run 'sageactive4j init', or pass --subscription-key / "
                    + "set SAGEACTIVE4J_SUBSCRIPTION_KEY.");
        }
        return new SageActive4jClient(config);
    }

    /** Version line for {@code --version}. */
    static final class Version implements IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[] {"sageactive4j CLI " + SageActive4j.version() + " (Java " + System.getProperty("java.version") + ")"};
        }
    }
}
