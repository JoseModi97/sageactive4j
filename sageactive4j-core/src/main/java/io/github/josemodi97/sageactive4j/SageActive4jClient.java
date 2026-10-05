package io.github.josemodi97.sageactive4j;

import io.github.josemodi97.sageactive4j.auth.SageAuthClient;
import io.github.josemodi97.sageactive4j.exception.SageActive4jApiException;
import io.github.josemodi97.sageactive4j.exception.SageActive4jGraphQLException;
import io.github.josemodi97.sageactive4j.graphql.FileUpload;
import io.github.josemodi97.sageactive4j.graphql.GraphQLError;
import io.github.josemodi97.sageactive4j.graphql.GraphQLRequest;
import io.github.josemodi97.sageactive4j.graphql.GraphQLResponse;
import io.github.josemodi97.sageactive4j.graphql.ResponseMapper;
import io.github.josemodi97.sageactive4j.internal.AsyncSupport;
import io.github.josemodi97.sageactive4j.internal.GraphQLTransport;
import io.github.josemodi97.sageactive4j.internal.Sleeper;
import io.github.josemodi97.sageactive4j.service.AccountingClient;
import io.github.josemodi97.sageactive4j.service.BanksClient;
import io.github.josemodi97.sageactive4j.service.CatalogClient;
import io.github.josemodi97.sageactive4j.service.FilesClient;
import io.github.josemodi97.sageactive4j.service.LocalizationClient;
import io.github.josemodi97.sageactive4j.service.OrganizationsClient;
import io.github.josemodi97.sageactive4j.service.ProductsClient;
import io.github.josemodi97.sageactive4j.service.PurchasesClient;
import io.github.josemodi97.sageactive4j.service.SalesClient;
import io.github.josemodi97.sageactive4j.service.ThirdPartiesClient;
import io.github.josemodi97.sageactive4j.service.UsersClient;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Entry point to the Sage Active Public API V2. Thread-safe: create one per
 * app (or per set of credentials), share it, and {@link #close()} it on
 * shutdown.
 *
 * <pre>{@code
 * SageActive4jClient sage = new SageActive4jClient(SageActive4jConfig.builder()
 *         .region(Region.FR)
 *         .subscriptionKey("...")
 *         .clientId("...").clientSecret("...")
 *         .refreshToken("...")
 *         .organizationId("...")
 *         .build());
 *
 * UserProfile me = sage.users().getProfile();
 * for (Customer c : sage.thirdParties().allCustomers(ListOptions.first(500))) { ... }
 * }</pre>
 *
 * <p>Domain clients cover the common operations; {@link #execute(GraphQLRequest)}
 * and {@link #query(String, Map)} reach anything else in the API.
 */
public final class SageActive4jClient implements AutoCloseable {

    private final SageActive4jConfig config;
    private final SageAuthClient auth;
    private final GraphQLTransport transport;
    private final AsyncSupport async;
    private final String organizationId;

    private final OrganizationsClient organizations;
    private final UsersClient users;
    private final AccountingClient accounting;
    private final ThirdPartiesClient thirdParties;
    private final ProductsClient products;
    private final SalesClient sales;
    private final PurchasesClient purchases;
    private final BanksClient banks;
    private final FilesClient files;
    private final CatalogClient catalog;
    private final LocalizationClient localization;

    /** @throws io.github.josemodi97.sageactive4j.exception.SageActive4jConfigurationException if the config is unusable */
    public SageActive4jClient(SageActive4jConfig config) {
        this(config, Clock.systemUTC(), Sleeper.SYSTEM);
    }

    /** Test seam: fixed clock and a non-blocking sleeper. */
    SageActive4jClient(SageActive4jConfig config, Clock clock, Sleeper sleeper) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        config.validate();
        this.config = config;
        this.auth = new SageAuthClient(config, clock);
        this.transport = new GraphQLTransport(config, auth, sleeper, clock);
        this.async = new AsyncSupport(config.getExecutor());
        this.organizationId = config.getOrganizationId();
        this.organizations = new OrganizationsClient(this);
        this.users = new UsersClient(this);
        this.accounting = new AccountingClient(this);
        this.thirdParties = new ThirdPartiesClient(this);
        this.products = new ProductsClient(this);
        this.sales = new SalesClient(this);
        this.purchases = new PurchasesClient(this);
        this.banks = new BanksClient(this);
        this.files = new FilesClient(this);
        this.catalog = new CatalogClient(this);
        this.localization = new LocalizationClient(this);
    }

    private SageActive4jClient(SageActive4jClient parent, String organizationId) {
        this.config = parent.config;
        this.auth = parent.auth;
        this.transport = parent.transport;
        this.async = parent.async;
        this.organizationId = organizationId;
        this.organizations = new OrganizationsClient(this);
        this.users = new UsersClient(this);
        this.accounting = new AccountingClient(this);
        this.thirdParties = new ThirdPartiesClient(this);
        this.products = new ProductsClient(this);
        this.sales = new SalesClient(this);
        this.purchases = new PurchasesClient(this);
        this.banks = new BanksClient(this);
        this.files = new FilesClient(this);
        this.catalog = new CatalogClient(this);
        this.localization = new LocalizationClient(this);
    }

    /** Shortcut to {@link SageActive4jConfig#builder()}. */
    public static SageActive4jConfig.Builder builder() {
        return SageActive4jConfig.builder();
    }

    /** Quick-start factory method mirroring {@code SageActiveClient.Create(...)}. */
    public static SageActive4jClient create(String subscriptionKey, String accessToken, Region region) {
        return new SageActive4jClient(SageActive4jConfig.builder()
                .subscriptionKey(subscriptionKey)
                .accessToken(accessToken)
                .region(region)
                .build());
    }

    /** Quick-start factory method with organization id. */
    public static SageActive4jClient create(String subscriptionKey, String accessToken, String organizationId, Region region) {
        return new SageActive4jClient(SageActive4jConfig.builder()
                .subscriptionKey(subscriptionKey)
                .accessToken(accessToken)
                .organizationId(organizationId)
                .region(region)
                .build());
    }

    /** A client from {@code SAGEACTIVE4J_*} environment variables. */
    public static SageActive4jClient fromEnvironment() {
        return new SageActive4jClient(SageActive4jConfig.fromEnvironment());
    }

    /**
     * A view of this client that sends a different {@code X-OrganizationId}.
     * Shares the connection settings, token cache, mutation limit and async
     * pool, so it's cheap - use it freely in multi-organization apps.
     * {@code null} sends no organization header.
     */
    public SageActive4jClient withOrganization(String organizationId) {
        return new SageActive4jClient(this, organizationId == null || organizationId.trim().isEmpty()
                ? null : organizationId.trim());
    }

    /** The organization this client (or view) sends, or {@code null}. */
    public String getOrganizationId() {
        return organizationId;
    }

    public SageActive4jConfig getConfig() {
        return config;
    }

    // ---------------------------------------------------------------- domains

    /** OAuth: sign-in URLs, code exchange, refresh, revocation. */
    public SageAuthClient auth() {
        return auth;
    }

    /** Organizations, their configuration, countries, currencies. */
    public OrganizationsClient organizations() {
        return organizations;
    }

    /** The signed-in user, the organization's users, permission checks. */
    public UsersClient users() {
        return users;
    }

    /** Fiscal years, chart of accounts, journals, journal entries. */
    public AccountingClient accounting() {
        return accounting;
    }

    /** Customers and suppliers. */
    public ThirdPartiesClient thirdParties() {
        return thirdParties;
    }

    /** Products and computed prices. */
    public ProductsClient products() {
        return products;
    }

    /** Sales invoices: create, close, post, settle. */
    public SalesClient sales() {
        return sales;
    }

    /** Supplier invoices: list, post, settle. */
    public PurchasesClient purchases() {
        return purchases;
    }

    /** Bank accounts, bank movements, reconciliation, payment methods. */
    public BanksClient banks() {
        return banks;
    }

    /** File attachments: upload, list, export. */
    public FilesClient files() {
        return files;
    }

    /** Analytics aggregations. */
    public CatalogClient catalog() {
        return catalog;
    }

    /** Localized messages for business error keys. */
    public LocalizationClient localization() {
        return localization;
    }

    // ---------------------------------------------------------------- raw GraphQL

    /**
     * Sends any GraphQL operation and returns the response as-is, including
     * GraphQL {@code errors} (not thrown) and any partial data.
     *
     * @throws SageActive4jApiException on HTTP-level failures (see subclasses)
     */
    public GraphQLResponse execute(GraphQLRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        return transport.execute(request, organizationId);
    }

    /** Runs a query or mutation without variables and returns its {@code data}. */
    public Map<String, Object> query(String query) {
        return query(query, null);
    }

    /**
     * Runs a query or mutation and returns its {@code data} as nested maps
     * and lists.
     *
     * @throws SageActive4jGraphQLException if the response carries GraphQL errors
     */
    public Map<String, Object> query(String query, Map<String, ?> variables) {
        return query(new GraphQLRequest(query, variables));
    }

    /** As {@link #query(String, Map)}, for a prepared request. */
    public Map<String, Object> query(GraphQLRequest request) {
        return dataOrThrow(execute(request));
    }

    /** As {@link #query(String, Map)}, converting {@code data} with {@code mapper}. */
    public <T> T query(String query, Map<String, ?> variables, ResponseMapper<T> mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("mapper must not be null");
        }
        return mapper.map(query(query, variables));
    }

    /**
     * Sends a GraphQL multipart request with one file (see
     * {@code files().upload(...)} for the common case) and returns its
     * {@code data}.
     *
     * @param variablePath where the file goes in the variables, e.g. {@code variables.input.file};
     *                     that variable must be {@code null} in {@code request}
     * @throws SageActive4jGraphQLException if the response carries GraphQL errors
     */
    public Map<String, Object> executeMultipart(GraphQLRequest request, String variablePath, FileUpload file) {
        if (request == null || file == null) {
            throw new IllegalArgumentException("request and file must not be null");
        }
        if (variablePath == null || !variablePath.startsWith("variables.")) {
            throw new IllegalArgumentException("variablePath must start with 'variables.', got: " + variablePath);
        }
        return dataOrThrow(transport.executeMultipart(request, variablePath, file, organizationId));
    }

    // ---------------------------------------------------------------- async

    /**
     * Runs any call off the caller's thread:
     * {@code sage.async(c -> c.sales().invoices(ListOptions.first(100)))}.
     * Uses the configured executor, else a small daemon pool owned by this
     * client. Failures complete the future exceptionally with the usual
     * SDK exceptions.
     */
    public <T> CompletableFuture<T> async(Function<SageActive4jClient, T> call) {
        if (call == null) {
            throw new IllegalArgumentException("call must not be null");
        }
        final SageActive4jClient self = this;
        return CompletableFuture.supplyAsync(() -> call.apply(self), async.executor());
    }

    /**
     * Shuts down the client-owned async pool (never a configured executor).
     * Idempotent; shared by every organization view of this client.
     */
    @Override
    public void close() {
        async.close();
    }

    private static Map<String, Object> dataOrThrow(GraphQLResponse response) {
        if (response.hasErrors()) {
            List<GraphQLError> errors = response.getErrors();
            StringBuilder message = new StringBuilder("Sage Active GraphQL error: ").append(errors.get(0));
            if (errors.size() > 1) {
                message.append(" (and ").append(errors.size() - 1).append(" more)");
            }
            throw new SageActive4jGraphQLException(message.toString(), response.getHttpStatus(),
                    response.getRawBody(), errors, response.getData());
        }
        if (response.getData() == null) {
            throw new SageActive4jApiException("Sage Active returned neither data nor errors",
                    response.getHttpStatus(), response.getRawBody());
        }
        return response.getData();
    }

    @Override
    public String toString() {
        return "SageActive4jClient{organizationId=" + organizationId + ", config=" + config + '}';
    }
}
