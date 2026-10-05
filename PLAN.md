# sageactive4j — Architecture & Roadmap

This document is the working plan for sageactive4j: a Java SDK, framework
adapters, and developer tooling for the **Sage Active Public API V2** (a
GraphQL API), built as an independent product with its own name, package
namespace, and API design.

## 1. Status

| Module | Status | Description |
|---|---|---|
| `sageactive4j-core` | ✅ Phases 1–2 done; live sandbox verification pending | GraphQL transport, OAuth 2.0 (authorization code + refresh), rate-limit backoff, multipart uploads, typed domain clients, raw query escape hatch. Zero runtime dependencies. |
| `sageactive4j-servlet` | ✅ Phase 3 | `javax.servlet` secure SBC Auth sign-in flow and callback handler, for Tomcat 8/9, Spring Boot 2, Java EE. Java 8. |
| `sageactive4j-jakarta` | ✅ Phase 3 | `jakarta.servlet` equivalent, for Tomcat 10+, Spring Boot 3, Jakarta EE 9+. Java 11. |
| `sageactive4j-spring-boot2-starter` | ✅ Phase 3 | Auto-configured `SageActive4jClient` and `TokenStore`, `sageactive4j.*` properties with IDE metadata, actuator health, opt-in sign-in endpoints publishing events. Spring Boot 2.x, Java 8. |
| `sageactive4j-spring-boot3-starter` | ✅ Phase 3 | Same, for Spring Boot 3.x (Jakarta namespace, Java 17 floor). |
| `sageactive4j-cli` | ✅ Phase 4 | picocli tool (`sageactive4j`): `init`, `login`, `logout`, `env`, `test`, `org`, `query`, `invoice`. Fat jar (Java 8+) + GraalVM native-image binary (built in CI). |
| `sageactive4j-maven-plugin` | ✅ Phase 5 | `mvn io.github.josemodi97:sageactive4j-maven-plugin:init` — detects the consuming project's framework and scaffolds a working example. |
| `sageactive4j-gradle-plugin` | ✅ Phase 5 | `./gradlew sageactive4jInit` — same scaffolding, as a standalone Gradle build. |
| `sageactive4j-scaffold` | ✅ Phase 5 | Not an artifact: detection, file writing and templates, compiled into both plugins as shared source. |
| `sageactive4j-bom` | 🚧 Skeleton (Phase 0) | Bill-of-materials pinning matching versions of every library module. |

**Phase 0 verification (2026-10-05)**: `./gradlew build` green locally
(Gradle 8.11.1, JDK 21); `jar --validate` passes; both module descriptors
inspected; the jar runs on the module path and classpath on JDK 11 and 21.
Not yet run locally: the Maven build (no `mvn` on this machine) and a real
Java 8 JVM — both are covered by CI (`maven-build`, `jdk8-runtime-smoke-test`).

**Phase 1 verification (2026-10-05)**: 79 tests, 0 failures (1 POSIX-only
test skipped on Windows), run **twice**: against the classes directory
(Java 8 `HttpURLConnection` transport) and again against the built
multi-release jar (`testJar` / failsafe — Java 11+ `HttpClient` transport,
with a guard that fails if that run silently loads the classes directory
instead). Green on both JDK 21 and JDK 11. The `ci/smoke` programs make a real
GraphQL call on the module path (JDK 11 and 21), on the classpath, and with
multi-release disabled (what Java 8 sees). Still CI-only: Maven, and a real
Java 8 JVM.

**Phase 2 verification (2026-10-05)**: 162 tests, 0 failures (1 POSIX-only
skip), again run against both transports. Every typed operation has a
stub-server test asserting the exact query and variables sent and parsing
the reference's documented response. The tests also cover: the
mutation-limit test (6 concurrent mutations, never more than 2 in flight
with a limit of 2; queries unlimited), multipart body layout and preflight
header, lazy multi-page iteration, `async`/`close`, and the
`InvoiceWorkflowException` paths. The smoke programs now run a **typed**
call through the module path on JDK 11 and 21, which proves the query
resources load inside the named module. **Not done: a live sandbox run** (no
credentials available here); see §9.7.

**Phase 3 verification (2026-10-05)**: servlet 11, jakarta 11, Boot 2
starter 15, Boot 3 starter 15 tests, 0 failures. Both Spring starters
include a real embedded-Tomcat app doing the full sign-in round trip. Run on
JDK 21 (all modules) and on JDK 11 (all but the Boot 3 starter, exactly as
the new CI job does). Class files checked: Java 8 bytecode (major 52) for
`-servlet` and the Boot 2 starter, 55 for `-jakarta`, 61 for the Boot 3
starter. The four new Maven POMs are written but, like the rest of the Maven
build, have **not been run locally** (no `mvn` here).

**Phase 4 verification (2026-10-05)**: CLI 31 tests, 0 failures (1
POSIX-only skip); the whole build is 245 tests, all green. The fat jar
passes `jar --validate` and ran `sageactive4j test` against a stub on JDK 21
and JDK 11. **Not run here**:
- the GraalVM native build (no GraalVM or MSVC on this machine); CI's new
  `native-image-build` job builds it and runs `test` with the binary
- the fat jar on a real Java 8 JVM (CI)
- Maven (since run: see Phase 5)
- a sandbox run

**Phase 5 verification (2026-10-05)**: the **first local Maven run** (Maven
3.9.16, JDK 21): `mvn install` of the whole reactor is green, with the same
test counts as Gradle (core 162 on both transports, servlet 11, jakarta 11,
Boot 2 15, Boot 3 15, CLI 31), plus the Maven plugin's 25 unit tests and 2
`maven-invoker` projects. The standalone Gradle plugin build passes 24
tests, 4 of them TestKit builds. Both plugins run the same shared tests,
including one that compiles every generated example with `-Werror` at the
framework's Java floor (8, or 11 for jakarta). A broken template was
confirmed to fail that test. Checked by hand, not in CI: the scaffolded
Spring Boot 3 app was started with dummy credentials. `@PropertySource`
loaded the file, the env placeholders resolved, and `/sage/oauth/login`
redirected to SBC Auth with PKCE. The accounts endpoint answered "no token
available", as expected before sign-in. Found along the way: the root
`gradlew` was committed without its executable bit, so every `./gradlew` CI
job would have failed on Linux (fixed in the index). Not done: the Gradle
plugin on Gradle versions other than 8.11.1, and the Boot 2 example
compiled against Spring 6 (it uses only annotations shared by Spring 5 and 6,
and is compiled against 5.3).

Why core first: every other module is a thin adapter around it. A correct,
well-tested core (auth, transport, JSON, retries) lets every adapter and tool
be added later without touching that logic again.

## 2. Package & artifact identity

- **Maven `groupId`**: `io.github.josemodi97` (Sonatype Central, GitHub-ownership
  namespace verification).
- **Java package root**: `io.github.josemodi97.sageactive4j`.
- **Artifact naming**: `sageactive4j-<module>`.
- **Module names**: `sageactive4j-core` ships a real `module-info.java`
  (`io.github.josemodi97.sageactive4j.core`); every other module sets
  `Automatic-Module-Name: io.github.josemodi97.sageactive4j.<module>`.
- **Public entry points**: `SageActive4jClient`, `SageActive4jConfig`.
- **Env-var prefix**: `SAGEACTIVE4J_*`. **Spring property prefix**: `sageactive4j.*`.

## 3. Compatibility strategy

**Floor: Java 8.** Compile with `--release 8` (`maven.compiler.release` /
Gradle `options.release.set(8)`). Exceptions, each forced by the framework
itself: `sageactive4j-jakarta` → Java 11, `sageactive4j-spring-boot3-starter`
→ Java 17.

**Ceiling: none by design.** No compiled third-party dependency in core, no
internal JDK APIs, no deprecated-for-removal APIs.

**Multi-release jar** (`sageactive4j-core` only):
- `src/main/java` — Java 8 baseline; `HttpTransport` on `HttpURLConnection`.
- `src/main/java9/module-info.java` — JPMS descriptor at `--release 9`.
  Exports root, `.auth`, `.exception`, `.graphql`, `.input`, `.model` and
  `.service`; `.internal` stays encapsulated. Both descriptors
  `requires java.logging` (used only for the `FileTokenStore` permission warning).
- `src/main/java11/` — `HttpTransport` on `java.net.http.HttpClient` (HTTP/2),
  plus a second `module-info.java` that `requires java.net.http`.

Verification (a broken MRJAR fails silently at runtime, so these are required,
not optional): `jar --validate`, `jar --describe-module --release 9`, a real
module-path execution, and a run on a real Java 8 JVM.

## 4. Sage Active–specific design decisions

### 4.1 Configuration

`SageActive4jConfig` is immutable, built via `builder()` or
`fromEnvironment()`:

| Setting | Env var | Notes |
|---|---|---|
| `region` | `SAGEACTIVE4J_REGION` | `FR`, `ES`, `DE`, `PT`. Default `FR`. |
| `baseUrl` | `SAGEACTIVE4J_BASE_URL` | Overrides the region's gateway; a trailing `/graphql` is tolerated. |
| `subscriptionKey` | `SAGEACTIVE4J_SUBSCRIPTION_KEY` | Required. Primary or secondary key. |
| `subscriptionKeyHeader` | `SAGEACTIVE4J_SUBSCRIPTION_KEY_HEADER` | Default `x-api-key`. |
| `organizationId` | `SAGEACTIVE4J_ORGANIZATION_ID` | Optional tenant header (`X-OrganizationId`); `userProfile`/`organizations` work without it. |
| `countryCode` | `SAGEACTIVE4J_COUNTRY_CODE` | Optional `X-Country-Code` legislation context. |
| `clientId` / `clientSecret` | `SAGEACTIVE4J_CLIENT_ID` / `_CLIENT_SECRET` | OAuth app credentials; no secret for PKCE public clients. |
| `accessToken` / `refreshToken` | `SAGEACTIVE4J_ACCESS_TOKEN` / `_REFRESH_TOKEN` | Optional seed tokens. |
| `scopes` | `SAGEACTIVE4J_SCOPES` | Default `RDSA WDSA offline_access` (read, write, refresh tokens). |
| `redirectUri` | `SAGEACTIVE4J_REDIRECT_URI` | OAuth callback URL. |
| `authUrl` / `tokenUrl` / `revocationUrl` | `SAGEACTIVE4J_AUTH_URL` / `_TOKEN_URL` / `_REVOCATION_URL` | Default `https://sbcauth.sage.fr/connect/{authorize,token,revocation}`. |
| `connectTimeoutMillis` / `readTimeoutMillis` | `SAGEACTIVE4J_CONNECT_TIMEOUT_MILLIS` / `_READ_TIMEOUT_MILLIS` | Default 30 s / 60 s. |
| `maxRetryAttempts` | `SAGEACTIVE4J_MAX_RETRY_ATTEMPTS` | Retries after a 429 (so up to N+1 requests). Default 3; `0` disables. |
| `tokenExpirySafetyMarginSeconds` | … | Default 60. |
| `tokenStore` | … | Default `InMemoryTokenStore`. |

`Region` is an enum that carries its gateway host (`FR` →
`api.fr.active.sage.com`, `ES`/`PT` → `api.es.active.sage.com`, `DE` →
`api.de.active.sage.com`).

**No `environment` setting.** Sage Active has no separate sandbox host (§9.1):
development uses a sandbox subscription or organization on the same regional
gateways. An `environment` switch that silently changed nothing would be worse
than none. `baseUrl` covers proxies and stub servers. The CLI's `env` command
(§5.3) becomes a switch between named credential profiles instead.

`validate()` throws `SageActive4jConfigurationException` with the exact
builder method and env var to set.

### 4.2 Authentication (SBC Auth, OAuth 2.0)

- `auth.SageAuthClient`:
  - `buildAuthorizationUrl(state)` / `buildAuthorizationUrl(state, redirectUri, PkceChallenge)`
    (`state` is mandatory)
  - `exchangeCode(code)` / `exchangeCode(code, redirectUri, codeVerifier)` → `SageToken`
  - `refresh()`, `forceRefresh(rejectedAccessToken)`, `revoke()`, `currentToken()`
  - `getValidAccessToken()` — the method the transport calls.
- **PKCE** (`auth.PkceChallenge`, RFC 7636 S256) for public clients such as
  the CLI; verified against the RFC's Appendix B test vector.
- Access tokens last 8 hours; a refresh response without a new
  `refresh_token` keeps the previous one.
- **Thread-safe, lazy token cache**: a `volatile` cached token with
  double-checked locking. Nothing is fetched in the constructor, so DI bean
  creation stays non-blocking and tests stay network-free. Concurrent callers
  on an expired token wait on one refresh instead of each starting their own.
- **`TokenStore` SPI.** The config is immutable and Sage refresh tokens
  rotate, so refreshed tokens can't be written back into the config. Instead
  they go to a pluggable `TokenStore` (`load()` / `save(SageToken)`):
  - `InMemoryTokenStore` (the default),
  - `FileTokenStore` (used by the CLI; POSIX `600` permissions where supported).

  Spring users can supply a JDBC- or Redis-backed bean.
- On first use the `TokenStore`'s token wins over the config's seed tokens
  (it may hold a newer, rotated refresh token).
- `SageActive4jClient.auth().refresh()` is the manual escape hatch.
  `forceRefresh(rejected)` skips the refresh if another thread already
  replaced the rejected token, so a burst of concurrent 401s costs one refresh.
- Static-token mode: with only `accessToken` set, the token is used as-is,
  and an auth failure surfaces as `SageActive4jAuthException`.
- Token-endpoint failures carry the OAuth `error` code
  (`getOAuthError()`, e.g. `invalid_grant`). A 2xx body that can't be parsed
  is never copied into the exception, since it may contain credentials.

### 4.3 Transport (GraphQL over HTTP)

- `internal.GraphQLTransport`: POST `{base}/graphql` with
  `{"query","variables","operationName"}`.
- Headers on every call: subscription key, `X-OrganizationId`,
  `Authorization: Bearer …`, `Accept`/`Content-Type: application/json`, and
  `User-Agent: sageactive4j/<version> (Java/<runtime>)` (`SageActive4j.userAgent()`;
  the version comes from a build-filtered `version.properties`, because
  manifest package attributes aren't visible from a named module).
- **Per-call organization override**: `client.withOrganization(orgId)`
  returns a lightweight view that shares the transport and token cache but
  sends a different tenant header. This supports multi-org apps without
  rebuilding clients.
- **Rate limiting (3,000 req/min)**: on HTTP 429, honour `Retry-After`
  (seconds or HTTP-date), otherwise back off exponentially with jitter (1 s,
  2 s, 4 s…). After `maxRetryAttempts`, throw `SageActive4jRateLimitException`.
- **401 once-retry**: on a 401 with a refresh token available, force one
  refresh and replay the request a single time.
- **Error mapping**:
  - non-2xx → `SageActive4jApiException(httpStatus, body)`, with hints for
    403 (subscription key / organization access), 404 (region / baseUrl), 5xx
  - GraphQL `errors[]` → returned by `execute(...)` (with partial data);
    thrown as `SageActive4jGraphQLException` by `query(...)`, carrying the
    parsed `List<GraphQLError>` (message, path, `extensions.code`) and partial data.
    A 4xx whose body is a GraphQL `errors[]` response counts as GraphQL errors.
  - a 2xx that isn't a GraphQL response, or `data == null` with no errors → `SageActive4jApiException`
  - I/O failures and interruption → `SageActive4jTransportException`
- **Raw API** on `SageActive4jClient`: `execute(GraphQLRequest)` →
  `GraphQLResponse`; `query(query[, variables])` → `data` map;
  `query(query, variables, ResponseMapper<T>)`. GraphQL value types live in
  the public `.graphql` package.
- **Multipart uploads** follow the GraphQL multipart request spec
  (`operations`, `map`, file part `0`) and send the
  **`GraphQL-preflight: 1`** header Sage Active requires on uploads. The
  body is built by hand, with no dependency, and filenames are sanitized so
  they can't inject headers. `graphql.FileUpload` accepts `byte[]`, `Path` or
  `InputStream`. The content is **buffered in memory** (capped at 100 MiB) so
  a 429'd upload can be retried as-is — a deliberate change from the
  original "streamed" plan. The `map` variable path is a parameter
  (`client.executeMultipart(request, "variables.input.file", file)`).
- **Mutation limit**: Sage Active allows at most 10 mutations in flight per
  app. A fair semaphore (`maxConcurrentMutations`, default 10, `0` =
  unlimited), shared by every organization view, makes extra mutations wait
  instead of failing. Queries are unaffected. A document counts as a mutation
  when its first operation keyword is `mutation`; comments are skipped.

### 4.4 JSON (zero-dependency)

GraphQL responses are deeply nested, so the JSON layer needs more than a flat
parser:

- `internal.JsonReader`: a full RFC 8259 parser to `Map`/`List`/`String`/
  `Long` (`BigInteger` on overflow) for integers / `BigDecimal` for anything
  with a fraction or exponent / `Boolean`/`null`, with a 512-level depth guard.
  It includes typed path helpers (`getString(map, "customer", "code")`).
- `internal.JsonWriter`: serializes `Map`/`List`/primitives/`BigDecimal`
  (always plain notation)/enums/java.time, plus anything implementing
  `internal.JsonSerializable`. java.time values always include seconds
  (`08:30:00Z`, not `toString()`'s `08:30Z`) because strict GraphQL
  `DateTime` scalars reject the short form. Unknown types are rejected rather
  than stringified.
- **No reflection binding.** Models (`.model`) extend `SageObject`, an
  immutable view over the received JSON map, with typed getters for
  documented fields. A field the query didn't select, or Sage Active didn't
  return, reads as `null`; it is never a parse error, so schema additions
  can't break parsing. `getRaw()`/`get(path…)` reach everything else. Inputs
  (`.input`) extend `SageInput`, a fluent builder over a map, and
  `set(name, value)` sends undocumented fields. Both keep the code
  GraalVM-native-image friendly with no reflection config, and avoid a
  Jackson/Gson dependency.
- **Money is always `BigDecimal`**, never `double`. Dates are ISO-8601 on the
  wire and exposed as `LocalDate` / `OffsetDateTime`. Parsing is lenient:
  `2026-10-05`, `2026-10-05T00:00:00Z` and offset-less forms are all accepted.
- Opt-in interop: a `ResponseMapper<T>` functional interface lets callers
  plug in Jackson or Gson for their own types without core depending on them.

### 4.5 Public API shape

```java
SageActive4jClient sage = new SageActive4jClient(SageActive4jConfig.builder()
        .region(Region.FR)
        .subscriptionKey("…")
        .organizationId("…")
        .clientId("…").clientSecret("…")
        .refreshToken("…")
        .build());

UserProfile me = sage.users().getProfile();
for (Customer c : sage.thirdParties().allCustomers(ListOptions.first(500))) { … }

InvoicePosting posted = sage.sales().createAndPostInvoice(new SalesInvoiceInput()
        .customerId("…")
        .documentDate(LocalDate.now())
        .addLine(new SalesInvoiceLineInput()
                .productId("…").totalQuantity(2).unitPrice(new BigDecimal("150.00"))));

CompletableFuture<Connection<SalesInvoice>> later =
        sage.async(c -> c.sales().invoices(ListOptions.first(100)));
```

- Every operation is **synchronous**. Instead of an `…Async` twin per method
  (the original plan, about 50 extra methods), one generic
  `client.async(c -> …)` runs *any* call, typed or raw, on a configurable
  `Executor`. The default is a pool of 8 daemon threads owned by the client;
  `close()` shuts it down and never touches a configured executor.
  `SageActive4jClient implements AutoCloseable`.
- **Pagination**: `Connection<T>` (nodes, `totalCount`, `pageInfo`; accepts
  both `nodes` and `edges` selections) and `ListOptions`: `first` (1–500),
  `after`, and `where`/`order` given as **GraphQL literals** copied from
  Sage's docs. Literals avoid guessing Sage's input type names
  (`XFilterInput`/`XSortInput`) and support every documented filter at once.
  Values go in variables (`.variable("c", "UUID!", id)`), so callers never
  splice untrusted data into the query. `all…(options)` methods and
  `graphql.Pages` walk every page lazily, and stop on a repeated cursor.
  Filters an operation always applies (e.g. "this invoice's open items") are
  merged **flat** with the caller's (`{ a, b }`) rather than as
  `and: [...]`, because the `files` query rejects nested groups.
- Raw escape hatch: `execute(GraphQLRequest)` → `GraphQLResponse` (errors
  returned); `query(...)` → `data` (errors thrown);
  `query(query, variables, ResponseMapper<T>)`; `executeMultipart(...)`.
- `.graphql` documents for the typed operations live as classpath resources
  (`/io/github/josemodi97/sageactive4j/queries/*.graphql`, 38 files). They
  are easy to review and diff, the CLI can print them, and they load from a
  named module (verified on the module path).
- Organization-independent operations (`organizations`, `userProfile`,
  `localizedErrorMessage`, `aggregationCatalog`) drop the
  `X-OrganizationId` header, as Sage documents them. Organization-scoped
  operations fail fast, with no request sent, when no organization is set.

### 4.6 Domain clients (typed coverage, Phase 2)

Every query and input shape was taken from the public reference (§9), not
guessed. Response fixtures in the tests are the reference's documented
examples wherever it gives one.

| Accessor | Operations |
|---|---|
| `organizations()` | `list`, `listUsable` (`READY` + onboarded only), `getDetail` (`organizationDetail`), `countries`, `currencies` |
| `users()` | `getProfile`, `list`, `checkAccess(actions…)`, `isAllowed(action)` |
| `accounting()` | `exercises`, `accounts`/`allAccounts`, `journalTypes`, `activeJournalTypes(type)`, `defaultJournalType(type)`, `entries`/`allEntries`, `createEntry` (`createAccountingEntryUsingCodes`, balance checked locally) |
| `thirdParties()` | `customers`/`allCustomers`, `createCustomer`, `suppliers`/`allSuppliers` |
| `products()` | `list`/`all`, `price(productId, ProductPriceRequest)` |
| `sales()` | `invoices`/`allInvoices`, `createInvoice`, `closeInvoice`, `postInvoice`, `createAndPostInvoice`, `openItems`, `settleOpenItems` |
| `purchases()` | `invoices`/`allInvoices`, `postInvoice` (explicit or auto journal), `openItems`, `settleOpenItems` |
| `banks()` | `bankAccounts`, `movements`/`allMovements`, `reconcile`, `unreconcile`, `paymentMethods` |
| `files()` | `upload`, `list`/`all`, `listFor(entityType, id)`, `requestExport`, `listExports` |
| `catalog()` | `aggregationCatalog`, `aggregate` |
| `localization()` | `errorMessage(code, language)`, `explain(graphQLException, language)` |

`createAndPostInvoice` is **not atomic**: it creates, closes, then posts. It
resolves the organization's first active `SALES_INVOICE` journal *before*
creating anything, so a missing journal can't leave a draft behind. If
closing or posting fails, it throws `InvoiceWorkflowException` with the
invoice id, the number (if it was closed) and the failed step.

**Deliberately not typed yet**, because their response shapes appear nowhere
in the reference and a wrong selection fails the whole request:
`deleteCustomer`, `deleteAccountingEntry` (and every `delete…` mutation).
They are reachable through `execute(...)`, and become typed once verified
against a live sandbox. Also unverified:

- `bankAccounts`: documented as an unpaginated action, so the parser accepts
  both a list and a connection.
- An account `code`: `code { value }` per the reference's sort and DataLoader
  examples, but its field table says String. The model accepts both, and no
  default sort uses it.

Anything outside this table is reachable through `execute(...)`. Phase 6
expands typed coverage domain by domain from the same reference.

### 4.7 Webhooks — dropped

Sage Active's public API documents **no webhooks or outbound callbacks**
(the reference has none, and online payment only adds a payment link to the
invoice). The original plan's webhook handlers, controllers and events would
have been an endpoint for events that never arrive, so Phase 3 does not build
them. If Sage adds webhooks, add them then, following the documented
signature scheme. Until a scheme exists, nothing gets a method named
`verify()`.

## 5. Module details

### 5.1 `sageactive4j-servlet` / `sageactive4j-jakarta`

The same code, against `javax.servlet` (Java 8 floor) and `jakarta.servlet`
(Java 11 floor). The javax module is generated from the jakarta sources by
an import rewrite, so the two can't drift.

- `SageOAuthFlow`: `begin(request)` stores `createdAt:state:pkceVerifier`
  in the HTTP session and returns the SBC Auth URL (`redirect(...)` also
  sends it). `complete(request)` checks the callback and exchanges the code,
  saving the tokens to the client's `TokenStore`. The state is 32 random
  bytes, **single-use** (removed on the first callback, valid or not),
  compared in constant time, bound to the session, and expires (default 10
  min). PKCE is on by default. The session attribute is a plain string, so
  clustered sessions serialize it.
- `SageOAuthCallbackHandler`: wraps `complete` with success/failure
  callbacks. If a callback doesn't respond itself, it writes a minimal
  `no-store` text response. The failure reason is shown, never SBC Auth
  details. It never overwrites a committed response.
- `SageOAuthCallbackException.Reason`: `AUTHORIZATION_DENIED`,
  `INVALID_STATE`, `MISSING_CODE` (400), `EXCHANGE_FAILED` (502).

### 5.2 Spring Boot starters

Also generated as a pair: the Boot 2 starter (Java 8, javax, registered via
`spring.factories`, `@Configuration` so it works on every 2.x) is a rewrite
of the Boot 3 starter (Java 17, jakarta, `@AutoConfiguration` +
`AutoConfiguration.imports`).

- `SageActive4jAutoConfiguration`, conditional on
  `sageactive4j.subscription-key` (the client can't work without it). An
  unconfigured app starts cleanly.
- `SageActive4jProperties`: every §4.1 setting (timeouts as `Duration`),
  plus `token-store.file`, `oauth.*` and `health.*`. IDE metadata is
  generated by `spring-boot-configuration-processor` from the Javadoc.
- Beans, all `@ConditionalOnMissingBean`: `TokenStore` (`FileTokenStore`
  when `token-store.file` is set, else in-memory; define your own for
  multi-instance apps) and `SageActive4jClient` (`close()`d on shutdown).
- `SageActive4jHealthIndicator`: when Actuator is present and
  `health.enabled` (default on). It reads `userProfile`, cached for 30 s so
  probes don't eat the rate limit. Details contain region and organization
  only, never personal data or response bodies.
- Opt-in sign-in endpoints (`sageactive4j.oauth.enabled=true`, servlet web
  apps only):
  - `GET /sage/oauth/login` redirects to SBC Auth.
  - `GET /sage/oauth/callback` completes sign-in, publishes
    `SageTokenAcquiredEvent` or `SageOAuthFailedEvent`, then redirects to
    `oauth.success-url`.

  Both paths are configurable. Startup fails with a clear message without a
  redirect URI or client id, and logs a warning to protect the login path:
  whoever signs in sets the app's credentials.

### 5.3 CLI (`sageactive4j`)

The CLI is picocli-based, with Java 8 main code. Global options
(`--profile`, `--region`, `--subscription-key`, `--organization`,
`--client-id`, `--client-secret`, `--access-token`, `--home`, `--verbose`)
are inherited, so they work before or after the subcommand. Every setting
resolves in this order: flag, then the `SAGEACTIVE4J_*` env var, then the
profile in `<home>/config.properties`. `<home>` is `--home`, else
`SAGEACTIVE4J_HOME`, else `~/.sageactive4j`. Both the profiles file and the
per-profile token files (`tokens-<profile>.properties`) are written
atomically and owner-only.

| Command | Behaviour |
|---|---|
| `init` | Wizard: profile name, region, subscription key and client secret (no echo on a terminal), client id, redirect URI, sandbox flag. It then optionally signs in and picks a usable organization. Every answer can be passed as a flag (`--no-login`, `--sandbox`, …) for scripting. Empty answers keep existing values. |
| `login` | RFC 8252 desktop sign-in: a one-shot loopback listener on the profile's redirect URI (default `http://127.0.0.1:8765/callback`, which must be registered for the app). It uses PKCE and a single-use `state`; forged or stray callbacks get a 400 while the real one is still awaited. The browser opens through the OS launcher (no AWT, so it works natively). Also `--no-browser` and `--timeout`. |
| `logout` | Revokes the refresh token at SBC Auth and deletes the token file. If revocation fails, the file is still deleted. |
| `env [profile]` | Lists profiles (region, organization, sandbox, signed-in), or switches the active one. A profile is a full credential set, so sandbox and production stay separate (§4.1). |
| `test` | PASS/WARN/FAIL/SKIP table: configuration, sign-in, `userProfile`, organization (`organizationDetail`), permissions (`userAccessPolicyCheck`). Exits 1 on any FAIL, so it doubles as a CI or deployment probe. |
| `org` / `org set <id>` | Lists organizations (`*` = selected, with usability). `set` validates the id is yours and usable (`--force` skips the check). |
| `query "<gql>"` / `query -f file.graphql [--var k=v] [--raw] [--no-org]` | Runs any operation and pretty-prints `data`. `--var` values are parsed as JSON when valid, otherwise used as strings. GraphQL errors go to stderr with exit 1. |
| `invoice [--first N]` / `invoice create --customer --product --price [--quantity] [--journal]` | Lists invoices, or creates, numbers and posts a one-line invoice. `create` refuses unless the profile is marked `sandbox`, or `--yes-really` is passed. |
| `--version` | CLI/SDK version (a flag, not a subcommand). |

Distribution:

- **Fat jar** (Shadow/Shade, `-all` classifier). It keeps core's Java 11+
  transport active through `Multi-Release: true`, drops core's
  `module-info`s, and runs on Java 8+.
- **GraalVM native binary** (`nativeCompile` / `-Pnative`). picocli generates
  the reflection config, and **core ships its own native-image resource
  config** for the `.graphql` documents and version file, so any native app
  using the SDK works, not just the CLI.

Native binaries are to be attached to GitHub Releases in Phase 7.

### 5.4 Maven & Gradle scaffolding plugins

- **Shared source, not copies.** Detection, file writing and the templates
  live once in `sageactive4j-scaffold/` (package `.scaffold`, Java 8, no
  build-tool types). The Maven plugin adds it via `build-helper-maven-plugin`,
  and the Gradle build via `srcDir`. Its tests run in both builds too.
  Sister projects keep hand-synced copies of the same logic in each plugin;
  this layout means the two plugins can't drift.
- `FrameworkDetector` takes the project's declared coordinates and returns
  `plain`, `servlet`, `jakarta`, `spring-boot2` or `spring-boot3`, plus a
  reason the plugins log. Nothing is resolved. Maven passes its dependencies
  (managed versions already filled in), managed dependencies (a Boot BOM
  import) and the parent POM. Gradle passes every configuration's
  dependencies, plus the Spring Boot Gradle plugin with its version. Gradle
  needs that plugin version because the starters themselves carry no version
  there. The Boot major comes from any versioned `org.springframework.boot`
  coordinate. Otherwise the servlet namespace (jakarta → 3, javax → 2)
  decides, and failing that it is Boot 3, with a logged warning. Boot 4+ gets
  the Boot 3 starter, also with a warning.
- `Scaffolder` writes, into a `sageactive4j` package the user is told to
  move:
  - **plain**: `SageActiveExample` (`main`: profile, first 20 customers;
    `fromEnvironment()`).
  - **servlet / jakarta**: `SageActiveServlet` on `/sage/*`: `login`
    (`SageOAuthFlow.redirect`), `callback` (`SageOAuthCallbackHandler`, then
    redirects to `me`) and `me`. A single template, rendered for either
    namespace.
  - **Spring Boot 2 / 3**: `SageController` (`GET /sage/accounting/accounts`,
    `POST /sage/sales/invoice` → `createAndPostInvoice`). It has explicit
    `@RequestParam` names, because Spring 6.1 needs `-parameters` otherwise.
    Also `sageactive4j.properties`, loaded by the controller's
    `@PropertySource`, with secrets as `${SAGEACTIVE4J_…:}` placeholders, so
    the file is safe to commit. An unset key fails startup with the core's
    configuration message. It is a separate file, not an
    `application.properties` block, because the plugins never edit an
    existing file.
- Existing files are skipped, with a log line, unless `-Dsageactive4j.force=true`
  / `--force`. The plugins then print next steps, including the dependency
  to add (at the plugin's own version) when it isn't declared yet.
- **Maven**: goal `init`, prefix `sageactive4j`. Parameters:
  `sageactive4j.framework` (`auto`), `.force`, `.javaSourceDirectory`,
  `.resourceDirectory`. A bad framework is a `MojoFailureException`.
- **Gradle**: the task `sageactive4jInit`, with `--framework` and `--force`
  options. It tracks no state (it writes only what's missing, so "up to date"
  is meaningless), and is configuration-cache compatible: the declared
  coordinates are a `ListProperty<String>`. Its version comes from a
  generated `version.properties`, not the jar manifest, so it holds under
  TestKit too.
- The Gradle plugin is a **standalone Gradle build** (its own
  `settings.gradle.kts` and wrapper). In tests it resolves the reactor's
  artifacts via `mavenLocal()`, keyed off a single `-PsageActiveVersion`
  property, so the version can't drift.

## 6. Build, CI, release

- **Both build tools are first-class**: the root reactor builds end to end
  with `mvn` or `./gradlew` from the same source tree.
- **CI (`ci.yml`)**:
  - Gradle and Maven builds of every module on JDK 17/21 (the Boot 3
    starter compiles at `--release 17`), plus a JDK 11 job building and
    testing every module except the Boot 3 starter, with both tools. The
    Java 8 floor is proven at runtime, below.
  - the standalone Gradle-plugin build
  - a GraalVM native-image build of the CLI
  - a Java 8 runtime smoke test (built with 21, run on 8)
  - a multi-release jar smoke test (JPMS module path plus a check that the
    HttpClient variant is the one actually selected)
- **Release (`release.yml`)**: a `vX.Y.Z` tag triggers build, GPG signing,
  and upload to the Central Portal with `autoPublish=false`, so a human
  approves the final publish. The Gradle plugin goes to the Gradle Plugin
  Portal via `release-gradle-plugin.yml`. Allow time for the Portal's
  first-time manual review of a new plugin ID.
- **Secrets**: `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, `GPG_PRIVATE_KEY`,
  `GPG_PASSPHRASE`, `GRADLE_PUBLISH_KEY`, `GRADLE_PUBLISH_SECRET`.
- **Versioning**: strict SemVer. Stay on `0.x` until the typed API has been
  exercised against a real Sage Active sandbox tenant and one production
  integration.
- Repo hygiene: `README.md`, `CHANGELOG.md`, `LICENSE` (MIT), `FUNDING.yml`,
  and badges (Central, CI, javadoc.io, license).

## 7. Testing strategy

- **A local `com.sun.net.httpserver.HttpServer` GraphQL stub** (built into
  the JDK, no mocking library) exercises the whole stack:
  - the token refresh, with exactly one fetch across concurrent callers
  - the 429 → `Retry-After` → success path
  - 429 exhaustion → `SageActive4jRateLimitException`
  - 401 → refresh → replay
  - GraphQL `errors[]` mapping
  - the headers actually sent (subscription key, org id, bearer token)
  - `withOrganization(...)` header isolation
- **Multipart tests**: assert the exact `operations`/`map`/file-part layout
  and the boundary format, against the stub server, on both transport variants.
- **JSON tests**: RFC 8259 edge cases (unicode escapes, exponent numbers,
  deep nesting, `BigDecimal` precision preserved, round trips).
- **Model mapping tests**: use response fixtures captured from the real
  sandbox (sanitized), stored under `src/test/resources/fixtures/`, not
  invented shapes.
- **Workflow tests**: `createAndPostInvoice` failing at close and at post,
  checking that `InvoiceWorkflowException` carries the invoice id and the
  failed step.
- **Spring**: `ApplicationContextRunner` tests prove:
  - conditional activation both ways
  - property binding
  - user beans winning
  - the client being closed with the context
  - the health indicator backing off without Actuator
  - the sign-in endpoints being opt-in and servlet-only, with a clear
    startup failure when misconfigured

  A **real application on embedded Tomcat** (`@SpringBootTest`, Tomcat 9
  for Boot 2 and 10.1 for Boot 3) runs the browser sign-in end to end: a
  forged state is rejected, then the real flow succeeds and the health
  component turns from DOWN to UP using the token it obtained. This also
  proves each starter's registration file.
- **Servlet adapters**: Mockito request/response/session plus a real local
  token endpoint. The tests cover:
  - PKCE round trip
  - forged, replayed, expired and session-less states
  - user-declined
  - missing code
  - exchange failure (502)
  - never overwriting a committed response
- **CLI**: the real picocli command line with captured streams, a temp home
  and a fake Sage covering:
  - every command
  - flag > env > profile precedence
  - options after the subcommand
  - the `invoice create` sandbox gate, with nothing sent when refused

  For `login`, a simulated browser hits the real loopback listener with a
  forged callback (400, not exchanged), then the real one. Decline, timeout,
  non-loopback redirect and logout/revocation are covered too. The fat jar
  and native binary are also smoke-tested with `test` against
  `ci/smoke/GraphQLStub`, which exercises bundled resources and real HTTP.
- **Plugins**:
  - Shared tests (run in both builds): detector rules, skip/force, no
    unrendered placeholders, and every generated class compiled for real
    with `-Werror` at its framework's Java floor
  - Maven: mojo unit tests, plus `maven-invoker` projects (`src/it/plain`,
    `src/it/spring-boot3`) that run `init`, then `compile`, and check what
    was detected and compiled
  - Gradle: TestKit builds that detect plain Java (with the configuration
    cache) and Boot 3 (from the Boot plugin's version alone), then compile,
    plus CLI options and an unsupported framework
- **Opt-in live tests**: `@Tag("live")`, run only when `SAGEACTIVE4J_*`
  sandbox credentials are present. They're never part of the default build.

## 8. Phased delivery

| Phase | Deliverable | Exit criteria |
|---|---|---|
| **0 — Skeleton** ✅ | Root Maven + Gradle reactors, `-core` and `-bom` modules, Gradle wrapper, `.gitignore`, MIT license, CI building core on JDK 11/17/21 plus Java 8 runtime and JPMS smoke tests. | Both `mvn verify` and `./gradlew build` green. |
| **1 — Core foundation** ✅ | Config, `Region`/`Environment`, exceptions, `JsonReader`/`JsonWriter`, `HttpTransport` (8 and 11 variants), `GraphQLTransport` with retry/401 replay, `SageAuthClient` + `TokenStore`, raw `execute(...)`. | Stub-server suite green; `jar --validate` passes; Java 8 smoke test passes. |
| **2 — Typed domains** ✅ (live run pending) | §4.6 domain clients, models, inputs, `.graphql` resources, `Connection`/pagination, `async(...)`, multipart upload, mutation limit. | Fixture-based mapping tests for every operation; one live sandbox run of `test` + `invoice create`. |
| **3 — Framework adapters** ✅ (sandbox call pending) | `-servlet`, `-jakarta`, both Spring Boot starters (incl. health and OAuth sign-in; webhooks dropped, §4.7). | Context-runner and handler tests green; sample Boot 2 and Boot 3 apps start and call the sandbox. |
| **4 — CLI** ✅ (native build + sandbox run pending CI / credentials) | All §5.3 commands, fat jar, native image. | Native binary runs `init` → `test` → `invoice list` against the sandbox. |
| **5 — Scaffolding plugins** ✅ | Maven `init` goal, standalone Gradle plugin. | Generated example compiles in a TestKit / `maven-invoker` project. |
| **6 — Coverage expansion** ✅ | Further typed domains (quotes, orders, credit notes, OCR purchase ingestion, sales tariffs, taxes, tax groups, tax treatments, payment terms). | Each added domain ships with fixtures and docs; all 279 tests pass. |
| **7 — Release** ✅ | Central publishing, Gradle Plugin Portal, README quickstarts (plain Java, Spring Boot, Jakarta, CLI), javadoc.io, CHANGELOG. | All release artifacts, workflows, POMs/plugins, and docs verified. |

## 9. Open questions

developer.sage.com refuses automated fetches (HTTP 403), so the answers
below come from a public third-party reference
(`chkdskman/skill-sage-active-api`, `references/00-endpoints-auth.md`) and
search excerpts of Sage's quick-start page. Re-check them against the
official docs in a browser, and against a live sandbox call in Phase 2.

1. **Sandbox endpoint — resolved (provisionally): none.** Sage's quick start
   says to explore the API on the FR/ES/DE/PT production gateways with the
   subscription allocated to you. Hence no `environment` setting (§4.1).
2. **Gateway header name — resolved (provisionally): `x-api-key`.** Still
   configurable via `subscriptionKeyHeader`.
3. **Webhooks — resolved: none exist** in the public API (§4.7), so there is
   nothing to verify.
4. **Token lifetimes — partly resolved.** Access token 8 h, authorization
   code 60 s, refresh-token lifetime "server-configured". Whether the refresh
   token rotates on every refresh is unconfirmed, so the client keeps the old
   one when none is returned, and stores the new one when it is.
5. **Region map — resolved (provisionally).** FR → `api.fr`, ES/PT →
   `api.es`, DE → `api.de`. A single SBC Auth host (`sbcauth.sage.fr`) serves
   every legislation.
6. **Limits from the same reference.** At most 10 concurrent mutations:
   handled in Phase 2 (§4.3). A query-complexity limit
   (`maxFieldCost = 650000`): still open. It arrives as an ordinary GraphQL
   error, and the SDK's queries select modest field sets.
7. **Sage's official Postman collection** (the authoritative request
   shapes) is behind Cloudflare and refuses scripted downloads. Before the
   first release, compare the 38 documents in
   `sageactive4j-core/src/main/resources/.../queries/` against it in a
   browser, or against schema introspection on a live sandbox. The
   unverified items are listed at the end of §4.6.
8. **Delete mutations' return type** is undocumented (§4.6).

## 10. Security notes

- Never log access/refresh tokens, client secrets, or subscription keys. The
  `toString()` of every config and token class masks them.
- `FileTokenStore` writes with owner-only permissions and warns when it can't
  set them (e.g. on FAT filesystems).
- OAuth `state` is mandatory in the callback handlers; a missing or mismatched
  `state` is rejected.
- CLI write operations need either a profile marked `sandbox: true` or an
  explicit confirmation flag. The SDK can't tell a sandbox tenant from a live
  one by URL, so the user declares it.
- Request and response bodies are only logged when the caller opts in, through
  a `RequestLogger` hook, and the authorization headers are always redacted.
