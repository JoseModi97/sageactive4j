package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.model.Organization;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** Lists the user's organizations, or selects one for the profile. */
@Command(name = "org", description = "List your organizations ('*' = selected), or select one with 'org set <id>'.",
        subcommands = OrgCommand.Set.class)
final class OrgCommand extends CliCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        try (SageActive4jClient client = root().client()) {
            List<Organization> organizations = client.organizations().list(ListOptions.first(ListOptions.MAX_PAGE_SIZE)).getNodes();
            if (organizations.isEmpty()) {
                out().println("This user has no organizations.");
                return 0;
            }
            Table table = new Table("", "ID", "NAME", "LEGISLATION", "STATUS", "USABLE");
            for (Organization o : organizations) {
                table.row(o.getId() != null && o.getId().equals(client.getOrganizationId()) ? "*" : "", o.getId(),
                        o.getSocialName(), o.getLegislationCode(), o.getStatus(), o.isUsable() ? "yes" : "no");
            }
            table.print(out());
            if (client.getOrganizationId() == null) {
                out().println("No organization selected yet: sageactive4j org set <id>");
            }
        }
        return 0;
    }

    /** {@code org set <id>}. */
    @Command(name = "set", description = "Select the organization this profile works in.")
    static final class Set extends CliCommand implements Callable<Integer> {

        @Parameters(index = "0", paramLabel = "ID", description = "Organization id (see 'org')")
        String id;

        @Option(names = "--force", description = "Save it even if it's not in your list or not usable")
        boolean force;

        @Override
        public Integer call() {
            Organization match = null;
            if (!force) {
                try (SageActive4jClient client = root().client()) {
                    for (Organization o : client.organizations().list(ListOptions.first(ListOptions.MAX_PAGE_SIZE))) {
                        if (id.equals(o.getId())) {
                            match = o;
                        }
                    }
                }
                if (match == null) {
                    throw new CliException("Organization " + id + " is not among yours (see 'sageactive4j org'); "
                            + "use --force to save it anyway.");
                }
                if (!match.isUsable()) {
                    throw new CliException("Organization " + match.getSocialName() + " is " + match.getStatus()
                            + (Boolean.TRUE.equals(match.getOnboardingCompleted()) ? "" : ", onboarding not completed")
                            + " - the API can't be used against it yet. Use --force to save it anyway.");
                }
            }
            ConfigFile file = root().configFile();
            String profile = root().profileName(file);
            file.set(profile, "organization-id", id);
            file.save();
            out().println("Profile '" + profile + "' now uses organization "
                    + (match == null ? id : match.getSocialName() + " (" + id + ")"));
            return 0;
        }
    }
}
