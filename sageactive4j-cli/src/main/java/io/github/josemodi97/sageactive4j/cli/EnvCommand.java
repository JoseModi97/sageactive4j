package io.github.josemodi97.sageactive4j.cli;

import java.nio.file.Files;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Lists profiles, or switches the active one. A profile is a full set of
 * credentials (subscription key, tokens, organization), so sandbox and
 * production stay fully separate - Sage Active has no separate sandbox host
 * to switch to.
 */
@Command(name = "env", description = "List profiles, or switch the active one (e.g. 'env sandbox').")
final class EnvCommand extends CliCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..1", paramLabel = "PROFILE", description = "Profile to make active")
    String target;

    @Override
    public Integer call() {
        ConfigFile file = root().configFile();
        if (target != null) {
            String name = ConfigFile.validProfileName(target);
            if (!file.hasProfile(name)) {
                throw new CliException("No profile '" + name + "'. Create it with: sageactive4j init --profile " + name);
            }
            file.setActiveProfile(name);
            file.save();
            out().println("Active profile: " + name);
            return 0;
        }
        if (file.profiles().isEmpty()) {
            out().println("No profiles yet. Create one with: sageactive4j init");
            return 0;
        }
        String active = root().profileName(file);
        Table table = new Table("", "PROFILE", "REGION", "ORGANIZATION", "SANDBOX", "SIGNED IN");
        for (String name : file.profiles()) {
            table.row(name.equals(active) ? "*" : "", name,
                    or(file.get(name, "region"), "FR"),
                    or(file.get(name, "organization-id"), "-"),
                    "true".equals(file.get(name, "sandbox")) ? "yes" : "no",
                    Files.isRegularFile(file.tokenFile(name)) ? "yes" : "no");
        }
        table.print(out());
        return 0;
    }

    private static String or(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
