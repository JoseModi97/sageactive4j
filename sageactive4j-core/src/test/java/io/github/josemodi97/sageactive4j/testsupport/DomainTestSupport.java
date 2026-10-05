package io.github.josemodi97.sageactive4j.testsupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.SageActive4jConfig;
import io.github.josemodi97.sageactive4j.TestClients;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import io.github.josemodi97.sageactive4j.testsupport.StubServer.Recorded;
import io.github.josemodi97.sageactive4j.testsupport.StubServer.Reply;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * A client wired to a stub GraphQL endpoint with a static token and
 * organization {@code org-1}, plus helpers to script replies and inspect
 * what the SDK sent.
 */
public abstract class DomainTestSupport {

    protected static final String GQL = "/graphql";
    protected static final String ORG = "org-1";

    protected StubServer server;
    protected SageActive4jClient sage;
    protected final RecordingSleeper sleeper = new RecordingSleeper();

    @BeforeEach
    void startStub() throws Exception {
        server = new StubServer();
        sage = TestClients.create(config().build(), new MutableClock(1_790_000_000_000L), sleeper);
    }

    @AfterEach
    void stopStub() {
        sage.close();
        server.close();
    }

    protected SageActive4jConfig.Builder config() {
        return SageActive4jConfig.builder()
                .baseUrl(server.url())
                .subscriptionKey("sub-key")
                .accessToken("token")
                .organizationId(ORG);
    }

    /** Queues {@code {"data": <dataJson>}}. */
    protected void respond(String dataJson) {
        server.enqueue(GQL, Reply.json("{\"data\":" + dataJson + "}"));
    }

    protected void respondErrors(String errorsJson) {
        server.enqueue(GQL, Reply.json("{\"errors\":" + errorsJson + ",\"data\":null}"));
    }

    protected Recorded request(int index) {
        List<Recorded> requests = server.requests(GQL);
        assertTrue(requests.size() > index, "expected at least " + (index + 1) + " GraphQL request(s), got " + requests.size());
        return requests.get(index);
    }

    protected Recorded lastRequest() {
        List<Recorded> requests = server.requests(GQL);
        assertTrue(!requests.isEmpty(), "no GraphQL request was sent");
        return requests.get(requests.size() - 1);
    }

    protected static Map<String, Object> body(Recorded request) {
        return JsonReader.parseObject(request.body);
    }

    /** The query text with whitespace collapsed, for readable {@code contains} checks. */
    protected static String query(Recorded request) {
        return ((String) body(request).get("query")).replaceAll("\\s+", " ");
    }

    protected static Map<String, Object> variables(Recorded request) {
        Map<String, Object> vars = JsonReader.getMap(body(request), "variables");
        return vars == null ? java.util.Collections.<String, Object>emptyMap() : vars;
    }

    protected static void assertContains(String haystack, String needle) {
        assertTrue(haystack.contains(needle), "expected to find\n  " + needle + "\nin\n  " + haystack);
    }
}
