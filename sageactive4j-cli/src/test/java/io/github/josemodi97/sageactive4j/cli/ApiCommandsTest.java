package io.github.josemodi97.sageactive4j.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.internal.JsonReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiCommandsTest extends CliTestSupport {

    private static final String PROFILE = "{\"userProfile\":{\"id\":\"u1\",\"fullName\":\"Ada Lovelace\"}}";
    private static final String ORGS = "{\"organizations\":{\"nodes\":["
            + "{\"id\":\"org-a\",\"socialName\":\"Ready Co\",\"legislationCode\":\"FR\",\"status\":\"READY\",\"onboardingCompleted\":true},"
            + "{\"id\":\"org-b\",\"socialName\":\"Expired Co\",\"legislationCode\":\"FR\",\"status\":\"EXPIRED\",\"onboardingCompleted\":true}"
            + "],\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":2}}";

    private Map<String, Object> variables(int call) {
        return JsonReader.getMap(JsonReader.parseObject(sage.graphql.get(call).body), "variables");
    }

    @Test
    void settingsPrecedenceIsFlagThenEnvironmentThenProfile() {
        profile("p", "access-token", "t");
        sage.on("userProfile", PROFILE);

        run("query", "--no-org", "{ userProfile { fullName } }");
        env.put("SAGEACTIVE4J_SUBSCRIPTION_KEY", "env-key");
        run("query", "--no-org", "{ userProfile { fullName } }");
        run("--subscription-key", "flag-key", "query", "--no-org", "{ userProfile { fullName } }");

        assertEquals("profile-key", sage.graphql.get(0).apiKey);
        assertEquals("env-key", sage.graphql.get(1).apiKey);
        assertEquals("flag-key", sage.graphql.get(2).apiKey);
    }

    @Test
    void optionsWorkAfterTheSubcommandToo() {
        profile("p", "access-token", "t");
        sage.on("userProfile", PROFILE);
        run("query", "--subscription-key", "late-flag", "--no-org", "{ userProfile { fullName } }");
        assertEquals("late-flag", sage.graphql.get(0).apiKey);
    }

    @Test
    void queryPrettyPrintsDataAndParsesVariables(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        profile("p", "access-token", "t", "organization-id", "org-a");
        sage.on("customers", "{\"customers\":{\"totalCount\":3}}");
        Path file = dir.resolve("q.graphql");
        Files.write(file, "query ($n: Int!, $c: String) { customers(first: $n) { totalCount } }".getBytes(StandardCharsets.UTF_8));

        Run r = run("query", "-f", file.toString(), "--var", "n=10", "--var", "c=\"C001\"", "--var", "plain=hello world");

        assertEquals(0, r.exitCode, r.toString());
        assertTrue(r.out.contains("\"totalCount\": 3"), r.out);
        Map<String, Object> vars = variables(0);
        assertEquals(10L, vars.get("n"));
        assertEquals("C001", vars.get("c"));
        assertEquals("hello world", vars.get("plain"));
        assertEquals("org-a", sage.graphql.get(0).organization);
    }

    @Test
    void graphQLErrorsGoToStderrWithExitCode1() {
        profile("p", "access-token", "t");
        sage.onErrors("broken", "[{\"message\":\"Syntax error\",\"extensions\":{\"code\":\"HC0011\"}}]");
        Run r = run("query", "{ broken }");
        assertEquals(1, r.exitCode);
        assertTrue(r.err.contains("Syntax error [HC0011]"), r.err);

        Run raw = run("query", "--raw", "{ broken }");
        assertTrue(raw.out.contains("\"errors\""), raw.out);
    }

    @Test
    void queryNeedsExactlyOneSource() {
        profile("p", "access-token", "t");
        assertEquals(1, run("query").exitCode);
        assertTrue(sage.graphql.isEmpty());
    }

    @Test
    void missingSubscriptionKeyIsExplained() {
        Run r = run("query", "{ x }");
        assertEquals(1, r.exitCode);
        assertTrue(r.err.contains("sageactive4j init"), r.err);
    }

    @Test
    void testCommandPassesEndToEnd() {
        profile("p", "access-token", "t", "organization-id", "org-a");
        sage.on("userProfile", PROFILE)
                .on("organizationDetail", "{\"organizationDetail\":{\"nodes\":[{\"id\":\"org-a\",\"socialName\":\"Ready Co\"}]}}")
                .on("userAccessPolicyCheck", "{\"userAccessPolicyCheck\":[{\"action\":\"customers\",\"isAllowed\":true},"
                        + "{\"action\":\"salesInvoices\",\"isAllowed\":true},{\"action\":\"createSalesInvoice\",\"isAllowed\":false},"
                        + "{\"action\":\"accountingEntries\",\"isAllowed\":true}]}");

        Run r = run("test");

        assertEquals(0, r.exitCode, r.toString());
        assertTrue(r.out.contains("Sage Active    PASS    signed in as Ada Lovelace"), r.out);
        assertTrue(r.out.contains("Organization   PASS    Ready Co"), r.out);
        assertTrue(r.out.contains("WARN    not allowed: createSalesInvoice"), r.out);
    }

    @Test
    void testCommandFailsWhenNotSignedIn() {
        profile("p");
        Run r = run("test");
        assertEquals(1, r.exitCode);
        assertTrue(r.out.contains("not signed in - run 'sageactive4j login'"), r.out);
        assertTrue(r.out.contains("Sage Active    SKIP"), r.out);
        assertTrue(sage.graphql.isEmpty());
    }

    @Test
    void orgListMarksTheSelectedOne() {
        profile("p", "access-token", "t", "organization-id", "org-a");
        sage.on("organizations", ORGS);
        Run r = run("org");
        assertEquals(0, r.exitCode, r.toString());
        assertTrue(r.out.contains("*  org-a  Ready Co"), r.out);
        assertTrue(r.out.contains("EXPIRED") && r.out.contains("no"), r.out);
        assertNull(sage.graphql.get(0).organization, "organizations is listed without X-OrganizationId");
    }

    @Test
    void orgSetValidatesThenSaves() {
        profile("p", "access-token", "t");
        sage.on("organizations", ORGS);

        assertEquals(1, run("org", "set", "org-unknown").exitCode);
        Run expired = run("org", "set", "org-b");
        assertEquals(1, expired.exitCode);
        assertTrue(expired.err.contains("EXPIRED"), expired.err);
        assertNull(ConfigFile.load(home).get("p", "organization-id"));

        assertEquals(0, run("org", "set", "org-a").exitCode);
        assertEquals("org-a", ConfigFile.load(home).get("p", "organization-id"));

        int before = sage.graphql.size();
        assertEquals(0, run("org", "set", "anything", "--force").exitCode);
        assertEquals(before, sage.graphql.size(), "--force must not need the API");
        assertEquals("anything", ConfigFile.load(home).get("p", "organization-id"));
    }

    @Test
    void invoiceListTable() {
        profile("p", "access-token", "t", "organization-id", "org-a");
        sage.on("salesInvoices", "{\"salesInvoices\":{\"nodes\":[{\"operationalNumber\":\"0038\",\"documentDate\":\"2026-10-05T00:00:00Z\","
                + "\"status\":\"Posted\",\"customer\":{\"socialName\":\"DUPONT SA\"},\"totalLiquid\":180.00},"
                + "{\"documentDate\":\"2026-10-04\",\"status\":\"Pending\",\"socialName\":\"ACME\",\"totalLiquid\":10}],"
                + "\"pageInfo\":{\"hasNextPage\":true},\"totalCount\":57}}");
        Run r = run("invoice", "--first", "2");
        assertEquals(0, r.exitCode, r.toString());
        assertTrue(r.out.contains("0038     2026-10-05  DUPONT SA  Posted   180.00"), r.out);
        assertTrue(r.out.contains("(draft)"), r.out);
        assertTrue(r.out.contains("2 of 57 shown"), r.out);
    }

    @Test
    void invoiceCreateIsRefusedOutsideASandboxProfile() {
        profile("p", "access-token", "t", "organization-id", "org-a");
        Run r = run("invoice", "create", "--customer", "c", "--product", "p", "--price", "10");
        assertEquals(1, r.exitCode);
        assertTrue(r.err.contains("not marked as a sandbox"), r.err);
        assertTrue(sage.graphql.isEmpty(), "nothing may be sent");
    }

    @Test
    void invoiceCreateOnASandboxProfile() {
        profile("p", "access-token", "t", "organization-id", "org-a", "sandbox", "true");
        sage.on("journalTypes", "{\"journalTypes\":{\"nodes\":[{\"id\":\"j1\",\"type\":\"SALES_INVOICE\"}],\"pageInfo\":{\"hasNextPage\":false}}}")
                .on("createSalesInvoice", "{\"createSalesInvoice\":{\"id\":\"inv-1\"}}")
                .on("closeSalesInvoice", "{\"closeSalesInvoice\":{\"id\":\"inv-1\",\"operationalNumber\":\"0039\"}}")
                .on("postSalesInvoice", "{\"postSalesInvoice\":{\"accountingEntryId\":\"e1\",\"accountingEntryNumber\":1517}}");

        Run r = run("invoice", "create", "--customer", "cust-1", "--product", "prod-1", "--quantity", "2", "--price", "75.50");

        assertEquals(0, r.exitCode, r.toString());
        assertTrue(r.out.contains("Created and posted invoice 0039 (id inv-1), ledger entry #1517"), r.out);
        Map<String, Object> line = (Map<String, Object>) JsonReader.getList(variables(1), "values", "lines").get(0);
        assertEquals(new BigDecimal("75.50"), JsonReader.toBigDecimal(line.get("unitPrice")));
        assertEquals(2L, line.get("totalQuantity"));
    }

    @Test
    void yesReallyOverridesTheSandboxGate() {
        profile("p", "access-token", "t", "organization-id", "org-a");
        sage.on("createSalesInvoice", "{\"createSalesInvoice\":{\"id\":\"inv-1\"}}")
                .on("closeSalesInvoice", "{\"closeSalesInvoice\":{\"id\":\"inv-1\",\"operationalNumber\":\"1\"}}")
                .on("postSalesInvoice", "{\"postSalesInvoice\":{\"accountingEntryId\":\"e\",\"accountingEntryNumber\":1}}");
        assertEquals(0, run("invoice", "create", "--customer", "c", "--product", "p", "--price", "1",
                "--journal", "j", "--yes-really").exitCode);
    }
}
