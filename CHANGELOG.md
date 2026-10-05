# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [0.1.0] - 2026-10-05

### Added
- Phase 6 domain coverage expansion:
  - `sales().quotes(...)`, `allQuotes(...)`, and `createQuote(...)`: strongly typed sales quotes query and creation.
  - `sales().orders(...)`, `allOrders(...)`, and `createOrder(...)`: strongly typed sales orders query and creation.
  - `sales().generateCreditNote(invoiceId)`: credit note generation from a posted sales invoice.
  - `products().tariffs(...)` and `allTariffs(...)`: sales tariff lists and stretch rules.
  - `accounting().taxes(...)`, `taxGroups(...)`, and `taxTreatments(...)`: tax definitions, tax groups, and fiscal VAT treatments.
  - `accounting().paymentTerms(...)` and `allPaymentTerms(...)`: commercial payment term schedules and maturity rules.
  - `purchases().uploadReceipt(...)` and `invoicesByFileId(...)`: automated OCR receipt ingestion via `AP_AUTOMATION`.
- `sageactive4j-maven-plugin` (`mvn io.github.josemodi97:sageactive4j-maven-plugin:init`)
  and the `io.github.josemodi97.sageactive4j` Gradle plugin (`./gradlew sageactive4jInit`).
  - Detect the project's framework (plain Java, javax/jakarta servlet,
    Spring Boot 2/3) from its declared dependencies, parent POM, BOM imports
    or Spring Boot Gradle plugin, and log the guess.
  - Write a working example: a `main` (plain), a sign-in servlet (servlet,
    jakarta), or a `SageController` plus `sageactive4j.properties` with
    secrets read from environment variables (Spring Boot).
  - Never overwrite existing files without `-Dsageactive4j.force=true` / `--force`.
  - List next steps, including the dependency to add when it's missing.
  - Both plugins compile the same shared source (`sageactive4j-scaffold`).
- `sageactive4j-cli`: the `sageactive4j` command (`init`, `login`, `logout`,
  `env`, `test`, `org`, `query`, `invoice`).
  - Named profiles with per-profile tokens; flag > env > profile precedence.
  - Loopback browser sign-in with PKCE.
  - A sandbox gate on `invoice create`.
  - Fat jar (Java 8+) and a GraalVM native binary.
- `sageactive4j-core` now ships a GraalVM resource config, so native images
  include its GraphQL documents.
- `sageactive4j-servlet` (javax, Java 8) and `sageactive4j-jakarta`
  (Java 11): `SageOAuthFlow` for browser sign-in (session-bound,
  single-use, expiring `state`; PKCE) and `SageOAuthCallbackHandler`.
- `sageactive4j-spring-boot2-starter` (Java 8) and
  `sageactive4j-spring-boot3-starter` (Java 17):
  - auto-configured `SageActive4jClient` and `TokenStore` from `sageactive4j.*`
  - generated IDE metadata
  - an actuator health component (cached, no personal data)
  - opt-in `/sage/oauth/login` and `/sage/oauth/callback` endpoints publishing
    `SageTokenAcquiredEvent` / `SageOAuthFailedEvent`
- No webhook support: Sage Active's public API has no webhooks.
- Typed domain clients: `organizations()`, `users()`, `accounting()`,
  `thirdParties()`, `products()`, `sales()`, `purchases()`, `banks()`,
  `files()`, `catalog()`, `localization()`. All shapes come from the public
  Sage Active reference; delete mutations are not typed yet.
- `sales().createAndPostInvoice(...)`: resolves the sales journal first, then
  creates, closes and posts. A failure after the invoice exists throws
  `InvoiceWorkflowException` (invoice id, number, failed step).
- Models (`.model`, lenient views over the JSON with `getRaw()`), inputs
  (`.input`, fluent, with `set(name, value)` for undocumented fields), and
  local validation (required fields, balanced accounting entries).
- `ListOptions` (paging, GraphQL-literal `where`/`order`, typed variables),
  `Connection`, and lazy `all…()` iteration via `Pages`.
- File uploads (GraphQL multipart, `GraphQL-preflight: 1`) and exports.
- `client.async(c -> ...)` with a client-owned or configured executor;
  `SageActive4jClient` is now `AutoCloseable`.
- `maxConcurrentMutations` (default 10, Sage's documented limit).
- `SageActive4jClient` with raw GraphQL access: `execute(GraphQLRequest)`,
  `query(...)`, `query(..., ResponseMapper)`, and per-call organization views
  via `withOrganization(...)`.
- `SageActive4jConfig` (builder, `fromEnvironment()`, masked `toString()`)
  and `Region` (FR/ES/DE/PT gateways).
- SBC Auth OAuth 2.0 (`SageAuthClient`): authorization URLs with mandatory
  `state` and optional PKCE, code exchange, lazy and thread-safe token caching
  and refresh, one refresh-and-replay on HTTP 401, and revocation.
- `TokenStore` SPI with `InMemoryTokenStore` and `FileTokenStore`
  (owner-only, atomic writes).
- HTTP 429 handling: `Retry-After` (seconds or HTTP-date) or exponential
  backoff with jitter, then `SageActive4jRateLimitException`.
- Exception hierarchy: configuration, transport, API, auth, rate-limit, GraphQL.
- Zero-dependency JSON reader/writer (exact `BigDecimal` money, full
  ISO-8601 dates).
- A Java 11+ `HttpClient` transport in the multi-release jar. The test suite
  runs against both the classes directory and the jar.
- Project skeleton: Maven and Gradle reactors, `sageactive4j-core`
  (multi-release jar with Java 9 and Java 11 module descriptors) and
  `sageactive4j-bom`.
- `SageActive4j.version()` / `SageActive4j.userAgent()`.
- CI (Gradle and Maven on JDK 11/17/21, Java 8 runtime smoke test,
  multi-release jar/JPMS smoke test, standalone Gradle plugin build) and a
  Maven Central release workflow.

### Fixed
- `gradlew` is now committed as executable, so `./gradlew` works on Linux CI runners.
