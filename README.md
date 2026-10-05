# sageactive4j

[![CI](https://github.com/JoseModi97/sageactive4j/actions/workflows/ci.yml/badge.svg)](https://github.com/JoseModi97/sageactive4j/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A dependency-free Java SDK for the **Sage Active Public API V2** (GraphQL), with
servlet, Jakarta, and Spring Boot adapters, a CLI, and Maven/Gradle scaffolding
plugins. Java 8+.

> **Status: early development.** The core SDK (auth, transport, typed
> domain clients) is built and tested against Sage's documented request and
> response shapes, but not yet against a live tenant. Nothing is published
> yet. See [PLAN.md](PLAN.md) for the architecture and roadmap.

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

## Building

Either build tool works from the same source tree; both need a JDK 11+ to
build (the output runs on Java 8+).

```bash
./gradlew build
```

```bash
mvn verify
```

## License

MIT © [Jose Modi](https://github.com/JoseModi97)
