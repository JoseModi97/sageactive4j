package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.model.AccessCheck;
import io.github.josemodi97.sageactive4j.model.OrganizationDetail;
import io.github.josemodi97.sageactive4j.model.UserProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * Checks the setup end to end and prints a PASS/WARN/FAIL/SKIP table.
 * Exits 1 if anything failed, so it also works as a CI or deployment probe.
 */
@Command(name = "test", description = "Check configuration, sign-in, organization and permissions.")
final class TestCommand extends CliCommand implements Callable<Integer> {

    /** Actions checked with userAccessPolicyCheck: the common read and write paths. */
    static final String[] ACTIONS = {"customers", "salesInvoices", "createSalesInvoice", "accountingEntries"};

    @Override
    public Integer call() {
        Table table = new Table("CHECK", "RESULT", "DETAIL");
        boolean failed = false;

        SageActive4jClient client;
        try {
            client = root().client();
            table.row("Configuration", "PASS", "region " + client.getConfig().getRegion()
                    + ", profile " + root().profileName(root().configFile()));
        } catch (RuntimeException e) {
            table.row("Configuration", "FAIL", SageActive4jCli.describe(e));
            skip(table, "Sign-in", "Sage Active", "Organization", "Permissions");
            table.print(out());
            return 1;
        }

        try {
            boolean signedIn = client.getConfig().getAccessToken() != null
                    || client.getConfig().getRefreshToken() != null || client.auth().currentToken() != null;
            if (!signedIn) {
                table.row("Sign-in", "FAIL", "not signed in - run 'sageactive4j login'");
                skip(table, "Sage Active", "Organization", "Permissions");
                table.print(out());
                return 1;
            }
            table.row("Sign-in", "PASS", "token available");

            try {
                UserProfile me = client.users().getProfile();
                table.row("Sage Active", "PASS", "signed in as " + me.getFullName());
            } catch (RuntimeException e) {
                table.row("Sage Active", "FAIL", SageActive4jCli.describe(e));
                skip(table, "Organization", "Permissions");
                table.print(out());
                return 1;
            }

            if (client.getOrganizationId() == null) {
                table.row("Organization", "WARN", "none selected - run 'sageactive4j org'");
                skip(table, "Permissions");
            } else {
                try {
                    OrganizationDetail detail = client.organizations().getDetail();
                    table.row("Organization", "PASS", detail.getSocialName() != null ? detail.getSocialName() : detail.getId());
                } catch (RuntimeException e) {
                    table.row("Organization", "FAIL", SageActive4jCli.describe(e));
                    failed = true;
                }
                try {
                    List<String> denied = new ArrayList<String>();
                    List<AccessCheck> checks = client.users().checkAccess(ACTIONS);
                    for (AccessCheck check : checks) {
                        if (!check.isAllowed()) {
                            denied.add(check.getAction());
                        }
                    }
                    if (denied.isEmpty()) {
                        table.row("Permissions", "PASS", checks.size() + "/" + ACTIONS.length + " checked actions allowed");
                    } else {
                        table.row("Permissions", "WARN", "not allowed: " + String.join(", ", denied));
                    }
                } catch (RuntimeException e) {
                    table.row("Permissions", "FAIL", SageActive4jCli.describe(e));
                    failed = true;
                }
            }
        } finally {
            client.close();
        }
        table.print(out());
        return failed ? 1 : 0;
    }

    private static void skip(Table table, String... checks) {
        for (String check : checks) {
            table.row(check, "SKIP", "");
        }
    }
}
