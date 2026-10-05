package io.github.josemodi97.sageactive4j.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.TestClients;
import io.github.josemodi97.sageactive4j.exception.SageActive4jGraphQLException;
import io.github.josemodi97.sageactive4j.graphql.FileUpload;
import io.github.josemodi97.sageactive4j.graphql.GraphQLRequest;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.model.Customer;
import io.github.josemodi97.sageactive4j.model.UserProfile;
import io.github.josemodi97.sageactive4j.testsupport.DomainTestSupport;
import io.github.josemodi97.sageactive4j.testsupport.MutableClock;
import io.github.josemodi97.sageactive4j.testsupport.StubServer.Reply;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PagingAndInfrastructureTest extends DomainTestSupport {

    private static String customersPage(String code, boolean hasNext, String cursor) {
        return "{\"customers\":{\"nodes\":[{\"id\":\"" + code + "\",\"code\":\"" + code + "\"}],"
                + "\"pageInfo\":{\"hasNextPage\":" + hasNext + ",\"endCursor\":" + (cursor == null ? "null" : "\"" + cursor + "\"")
                + "},\"totalCount\":3}}";
    }

    @Test
    void iterationFollowsCursorsLazily() {
        respond(customersPage("A", true, "c1"));
        respond(customersPage("B", true, "c2"));
        respond(customersPage("C", false, "c3"));

        List<String> codes = new ArrayList<String>();
        for (Customer customer : sage.thirdParties().allCustomers(ListOptions.first(1))) {
            codes.add(customer.getCode());
        }

        assertEquals(java.util.Arrays.asList("A", "B", "C"), codes);
        assertNull(variables(request(0)).get("after"));
        assertEquals("c1", variables(request(1)).get("after"));
        assertEquals("c2", variables(request(2)).get("after"));
    }

    @Test
    void breakingOutEarlyFetchesNoFurtherPages() {
        respond(customersPage("A", true, "c1"));
        for (Customer ignored : sage.thirdParties().allCustomers(ListOptions.first(1))) {
            break;
        }
        assertEquals(1, server.requests().size());
    }

    @Test
    void aRepeatedCursorStopsIteration() {
        respond(customersPage("A", true, "same"));
        respond(customersPage("B", true, "same"));
        int count = 0;
        for (Customer ignored : sage.thirdParties().allCustomers(ListOptions.first(1))) {
            count++;
        }
        assertEquals(2, count);
        assertEquals(2, server.requests().size());
    }

    @Test
    void listOptionsGuardTheirInputs() {
        assertThrows(IllegalArgumentException.class, () -> ListOptions.first(0));
        assertThrows(IllegalArgumentException.class, () -> ListOptions.first(501));
        assertThrows(IllegalArgumentException.class, () -> ListOptions.first(1).variable("bad-name", "UUID", "x"));
        assertThrows(IllegalArgumentException.class, () -> ListOptions.first(1).variable("first", "Int", 1));
        assertThrows(IllegalArgumentException.class, () -> ListOptions.first(1).variable("x", "UUID) { evil }", "x"));
        ListOptions original = ListOptions.first(10);
        ListOptions changed = original.after("c").where("{ a: { eq: 1 } }");
        assertNull(original.getAfter(), "ListOptions are immutable");
        assertEquals("c", changed.getAfter());
    }

    @Test
    void variableNamesCannotCollideWithAnOperationsOwn() {
        assertThrows(IllegalArgumentException.class, () -> sage.banks().movements("b1",
                ListOptions.first(1).variable("bankAccountId", "UUID", "other")));
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void mutationsAreLimitedButQueriesAreNot() throws Exception {
        SageActive4jClient limited = TestClients.create(config().maxConcurrentMutations(2).build(),
                new MutableClock(0), sleeper);
        server.always(GQL, Reply.json("{\"data\":{\"ok\":true}}").delay(150));

        ExecutorService pool = Executors.newFixedThreadPool(6);
        List<Future<?>> futures = new ArrayList<Future<?>>();
        for (int i = 0; i < 6; i++) {
            futures.add(pool.submit(() -> limited.query("mutation { ok }")));
        }
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        assertEquals(2, server.maxInFlight(), "at most 2 mutations may be in flight");

        futures.clear();
        for (int i = 0; i < 6; i++) {
            futures.add(pool.submit(() -> limited.query("query { ok }")));
        }
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        assertTrue(server.maxInFlight() > 2, "queries are not limited, got max " + server.maxInFlight());
        pool.shutdown();
        limited.close();
    }

    @Test
    void asyncRunsOnTheOwnedPoolAndCloseStopsIt() throws Exception {
        respond("{\"userProfile\":{\"fullName\":\"Async Ada\"}}");
        AtomicReference<String> thread = new AtomicReference<String>();
        CompletableFuture<UserProfile> future = sage.async(c -> {
            thread.set(Thread.currentThread().getName());
            return c.users().getProfile();
        });
        assertEquals("Async Ada", future.get(10, TimeUnit.SECONDS).getFullName());
        assertTrue(thread.get().startsWith("sageactive4j-async-"), thread.get());

        sage.close();
        assertThrows(IllegalStateException.class, () -> sage.async(c -> 1));
    }

    @Test
    void asyncFailuresCompleteExceptionally() {
        respondErrors("[{\"message\":\"nope\"}]");
        CompletableFuture<UserProfile> future = sage.async(c -> c.users().getProfile());
        ExecutionException e = assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
        assertTrue(e.getCause() instanceof SageActive4jGraphQLException);
    }

    @Test
    void aConfiguredExecutorIsUsedAndNeverShutDown() throws Exception {
        ExecutorService mine = Executors.newSingleThreadExecutor(r -> new Thread(r, "mine"));
        SageActive4jClient client = TestClients.create(config().executor(mine).build(), new MutableClock(0), sleeper);
        assertEquals("mine", client.async(c -> Thread.currentThread().getName()).get(10, TimeUnit.SECONDS));
        client.close();
        assertTrue(!mine.isShutdown());
        mine.shutdown();
    }

    @Test
    void organizationViewsShareTheAsyncPool() throws Exception {
        SageActive4jClient view = sage.withOrganization("org-2");
        assertEquals("org-2", view.async(SageActive4jClient::getOrganizationId).get(10, TimeUnit.SECONDS));
        view.close();
        assertThrows(IllegalStateException.class, () -> sage.async(c -> 1), "views share the parent's pool");
    }

    @Test
    void multipartRequiresAVariablesPath() {
        assertThrows(IllegalArgumentException.class, () -> sage.executeMultipart(
                new GraphQLRequest("mutation { x }"), "input.file", FileUpload.of("a.txt", new byte[0])));
    }

    @Test
    void domainClientsAreBoundToTheirView() {
        assertSame(sage.sales(), sage.sales());
        assertEquals("org-9", sage.withOrganization("org-9").getOrganizationId());
    }
}
