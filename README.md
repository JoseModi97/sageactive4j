# sageactive4j

[![CI](https://github.com/JoseModi97/sageactive4j/actions/workflows/ci.yml/badge.svg)](https://github.com/JoseModi97/sageactive4j/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.josemodi97/sageactive4j-core.svg)](https://central.sonatype.com/artifact/io.github.josemodi97/sageactive4j-core)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.josemodi97.sageactive4j.svg)](https://plugins.gradle.org/plugin/io.github.josemodi97.sageactive4j)
[![Javadoc](https://javadoc.io/badge2/io.github.josemodi97/sageactive4j-core/javadoc.svg)](https://javadoc.io/doc/io.github.josemodi97/sageactive4j-core)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A dependency-free Java SDK for the **Sage Active Public API V2** (GraphQL), with
servlet, Jakarta, and Spring Boot adapters, a CLI, and Maven/Gradle scaffolding
plugins. Java 8+.

## Installation

### Maven
Import the BOM to align module versions:
```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.josemodi97</groupId>
      <artifactId>sageactive4j-bom</artifactId>
      <version>0.1.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>io.github.josemodi97</groupId>
    <artifactId>sageactive4j-core</artifactId>
  </dependency>
</dependencies>
```

### Gradle
```kotlin
dependencies {
    implementation(platform("io.github.josemodi97:sageactive4j-bom:0.1.0"))
    implementation("io.github.josemodi97:sageactive4j-core")
}
```

## Quick look

```java
try (SageActive4jClient sage = new SageActive4jClient(SageActive4jConfig.builder()
        .region(Region.FR)
        .subscriptionKey("YOUR_SUBSCRIPTION_KEY")
        .clientId("YOUR_CLIENT_ID")
        .clientSecret("YOUR_CLIENT_SECRET")
        .refreshToken("YOUR_REFRESH_TOKEN")
        .tokenStore(new FileTokenStore(Paths.get("sage-tokens.properties")))
        .build())) {

    System.out.println("Connected as " + sage.users().getProfile().getFullName());

    // pick an organization the API may be used against
    SageActive4jClient org = sage.withOrganization(sage.organizations().listUsable().get(0).getId());

    // every customer, page by page, fetched lazily
    for (Customer c : org.thirdParties().allCustomers(ListOptions.first(500))) {
        System.out.println(c.getCode() + " " + c.getSocialName());
    }

    // create, number and post an invoice on the default sales journal
    InvoicePosting posted = org.sales().createAndPostInvoice(new SalesInvoiceInput()
            .customerId("CUSTOMER_ID")
            .documentDate(LocalDate.now())
            .addLine(new SalesInvoiceLineInput()
                    .productId("PRODUCT_ID").totalQuantity(2).unitPrice(new BigDecimal("150.00"))));
    System.out.println("Invoice " + posted.getOperationalNumber());

    // anything not typed yet: raw GraphQL
    Map<String, Object> data = org.query("{ taxes(first: 10) { nodes { id name } } }");
}
```

Or configure it entirely from `SAGEACTIVE4J_*` environment variables with
`SageActive4jClient.fromEnvironment()`.

## Spring Boot

Add `sageactive4j-spring-boot3-starter` (or `-spring-boot2-starter`), then:

```properties
sageactive4j.region=FR
sageactive4j.subscription-key=${SAGE_SUBSCRIPTION_KEY}
sageactive4j.client-id=${SAGE_CLIENT_ID}
sageactive4j.client-secret=${SAGE_CLIENT_SECRET}
sageactive4j.organization-id=${SAGE_ORGANIZATION_ID}
sageactive4j.redirect-uri=https://app.example.com/sage/oauth/callback
sageactive4j.token-store.file=/var/lib/myapp/sage-tokens.properties

# browser sign-in at /sage/oauth/login - protect that path (e.g. Spring Security):
# whoever signs in sets the credentials the whole app uses
sageactive4j.oauth.enabled=true
sageactive4j.oauth.success-url=/admin
```

Then inject `SageActive4jClient` anywhere. With Actuator on the classpath,
`/actuator/health` gains a `sageActive4j` component.

## Jakarta & javax Servlet

Add `sageactive4j-jakarta` (Jakarta EE 9+, Spring 6, Tomcat 10+) or `sageactive4j-servlet` (Java EE 8, Spring 5, Tomcat 9-) to handle SBC Auth browser login with PKCE and session-bound CSRF protection:

```java
SageAuthClient auth = new SageAuthClient(config);
URI redirectUri = URI.create("https://app.example.com/sage/callback");

// 1. In your login servlet: redirect user to Sage with PKCE challenge
SageOAuthFlow flow = new SageOAuthFlow(auth, redirectUri);
flow.start(request, response);

// 2. In your callback servlet: validate state, exchange code, and persist tokens
SageOAuthCallbackHandler handler = new SageOAuthCallbackHandler(
    auth,
    redirectUri,
    token -> tokenStore.store(token)
);
handler.handle(request, response);
```

## Command line

```bash
java -jar sageactive4j-cli-<version>-all.jar init
```

`init` asks for your app's keys and signs you in through the browser, then
lets you pick an organization. After that:

| Command | What it does |
|---|---|
| `sageactive4j test` | checks configuration, sign-in, organization and permissions |
| `sageactive4j org` / `org set <id>` | lists organizations / selects one |
| `sageactive4j query "{ userProfile { fullName } }"` | runs any GraphQL operation |
| `sageactive4j invoice` / `invoice create ...` | lists invoices / creates one (sandbox profiles only) |
| `sageactive4j env [profile]` | lists profiles / switches between them |
| `sageactive4j login` / `logout` | signs in again / revokes and deletes tokens |

The CLI redirects your browser to `http://127.0.0.1:8765/callback` after
sign-in, so register that redirect URI for your app in the Sage Developer
Center.

## Scaffold an example

Run this in your own project to get a working starting point:

```bash
mvn io.github.josemodi97:sageactive4j-maven-plugin:init
```

Or, with Gradle, apply `id("io.github.josemodi97.sageactive4j")` and run:

```bash
./gradlew sageactive4jInit
```

The plugin detects your framework from your dependencies and writes:

| Framework | Generated |
|---|---|
| plain Java | `sageactive4j/SageActiveExample.java`: prints your profile and first customers |
| javax / jakarta servlet | `sageactive4j/SageActiveServlet.java`: sign-in at `/sage/login`, then `/sage/me` |
| Spring Boot 2 / 3 | `sageactive4j/SageController.java` (`GET /sage/accounting/accounts`, `POST /sage/sales/invoice`) and `sageactive4j.properties` |

It then prints the remaining steps, including the dependency to add if you
don't have it yet. To pick the framework yourself, pass
`-Dsageactive4j.framework=jakarta` (Maven) or `--framework=jakarta` (Gradle).
Existing files are kept unless you add `-Dsageactive4j.force=true` / `--force`.

## Building

Either build tool works from the same source tree; both need a JDK 11+ to
build (the output runs on Java 8+).

```bash
./gradlew build
```

```bash
mvn verify
```

The Gradle plugin is a separate build. Its tests compile against the main
build's artifacts, so publish those first:

```bash
./gradlew publishToMavenLocal && cd sageactive4j-gradle-plugin && ./gradlew build
```

## License

MIT © [Jose Modi](https://github.com/JoseModi97)
