# sageactive4j - Sage Active Public API V2 Client for Java & Spring Boot

[![CI](https://github.com/JoseModi97/sageactive4j/actions/workflows/ci.yml/badge.svg)](https://github.com/JoseModi97/sageactive4j/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.josemodi97/sageactive4j-core.svg)](https://central.sonatype.com/artifact/io.github.josemodi97/sageactive4j-core)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.josemodi97.sageactive4j.svg)](https://plugins.gradle.org/plugin/io.github.josemodi97.sageactive4j)
[![Javadoc](https://javadoc.io/badge2/io.github.josemodi97/sageactive4j-core/javadoc.svg)](https://javadoc.io/doc/io.github.josemodi97/sageactive4j-core)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%20%7C%2011%20%7C%2017%20%7C%2021+-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7%20%7C%203.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Sponsor on Pesapal](https://img.shields.io/badge/Sponsor_via-Pesapal-0099ff.svg?logo=heart&logoColor=white)](https://store.pesapal.com/opensourcesponsorship)

[![Sponsor This Project via Pesapal](https://img.shields.io/badge/%E2%9D%A4%EF%B8%8F_Sponsor_This_Project-Pesapal_Open_Source-0099ff?style=for-the-badge&logo=heart&logoColor=white)](https://store.pesapal.com/opensourcesponsorship)

An all-in-one Java SDK and CLI companion for **Sage Active Public API V2** (GraphQL). Zero runtime dependencies in core, automated OAuth 2.0 SBC Auth token handling with PKCE (RFC 8252), rate limiting resilience (3,000 req/min), multi-legislation support (France, Spain, Germany, Portugal), multi-release JAR (Java 8 HttpURLConnection, Java 11+ HTTP/2 HttpClient, JPMS Java 9 module descriptor), Spring Boot 2 & 3 starters, and a rich CLI suite.

Authored by [Jose Modi](https://github.com/JoseModi97).

---

## Table of Contents

- [Highlights](#highlights)
- [Module Suite](#module-suite)
- [Installation](#installation)
- [Configuring Environments: Production vs Sandbox](#configuring-environments-production-vs-sandbox)
- [Zero-Code CLI Tool (`sageactive4j`)](#zero-code-cli-tool-sageactive4j)
- [Quickstart: Plain Java](#quickstart-plain-java)
- [Quickstart: Spring Boot 3 & 2](#quickstart-spring-boot-3--2)
- [Quickstart: Jakarta & javax Servlet](#quickstart-jakarta--javax-servlet)
- [Scaffold a Starter Project (Maven & Gradle Plugins)](#scaffold-a-starter-project-maven--gradle-plugins)
- [Core Architecture & Services](#core-architecture--services)
- [Java Compatibility Matrix](#java-compatibility-matrix)
- [Sponsorship](#sponsorship)
- [License](#license)

---

## Highlights

* **Zero Core Dependencies**: `sageactive4j-core` contains zero third-party dependencies, guaranteeing zero transitive dependency conflicts and minimal footprint.
* **100% Wire Coverage**: Covers General Ledger, Chart of Accounts, Sales Invoices, Quotes, Orders, Credit Notes, Purchase Invoices, Banking, Reconciliation, Tariffs, Taxes, Third Parties (Customers & Suppliers), OCR file uploads, and Analytics Cubes.
* **Multi-Release JAR (MR-JAR)**: Java 8 runtime floor (`--release 8` bytecode via `HttpURLConnection`), seamless Java 11+ upgrade (`java.net.http.HttpClient` with HTTP/2 multiplexing), and Java 9 JPMS descriptor (`io.github.josemodi97.sageactive4j.core`).
* **First-Class Spring Boot**: Dedicated starters for Spring Boot 3.x (`jakarta.servlet`, Java 17+) and Spring Boot 2.7.x (`javax.servlet`, Java 8+) with auto-configuration and Spring Boot Actuator health indicators.
* **Production-Ready Resilience**: Automated OAuth 2.0 SBC Auth token acquisition and refresh, thread-safe locking, exponential backoff on HTTP 429 rate limits, and mutation concurrency semaphore.
* **All-In-One Terminal CLI Tool**: Standalone fat JAR and GraalVM native binary with interactive onboarding wizard, connectivity diagnostic self-tests, profile switcher, and tools covering invoices, quotes, orders, accounts, customers, bank movements, and OCR file uploads.

---

## Module Suite

| Artifact ID | Description | Targets |
|---|---|---|
| [`sageactive4j-bom`](https://central.sonatype.com/artifact/io.github.josemodi97/sageactive4j-bom) | Bill of Materials (BOM) aligning all dependency versions | Maven / Gradle |
| [`sageactive4j-core`](https://central.sonatype.com/artifact/io.github.josemodi97/sageactive4j-core) | Core SDK: GraphQL transport, SBC Auth, entities, and 11 domain clients | Java 8, 11, 17, 21+ |
| [`sageactive4j-spring-boot3-starter`](https://central.sonatype.com/artifact/io.github.josemodi97/sageactive4j-spring-boot3-starter) | Spring Boot 3.x starter (`jakarta.servlet`, Actuator health indicator) | Java 17+ |
| [`sageactive4j-spring-boot2-starter`](https://central.sonatype.com/artifact/io.github.josemodi97/sageactive4j-spring-boot2-starter) | Spring Boot 2.7.x starter (`javax.servlet`, Actuator health indicator) | Java 8+ |
| [`sageactive4j-jakarta`](https://central.sonatype.com/artifact/io.github.josemodi97/sageactive4j-jakarta) | Jakarta EE 9+ servlet filter and OAuth 2.0 PKCE browser sign-in handler | Java 11+ |
| [`sageactive4j-servlet`](https://central.sonatype.com/artifact/io.github.josemodi97/sageactive4j-servlet) | Java EE 8 (`javax.servlet`) filter and OAuth 2.0 PKCE browser sign-in handler | Java 8+ |
| [`sageactive4j-cli`](https://central.sonatype.com/artifact/io.github.josemodi97/sageactive4j-cli) | Terminal companion tool, available as fat JAR or GraalVM native binary | CLI / GraalVM |
| [`sageactive4j-maven-plugin`](https://central.sonatype.com/artifact/io.github.josemodi97/sageactive4j-maven-plugin) | Maven plugin with project scaffolding goal (`sageactive4j:init`) | Maven 3.6+ |
| [`io.github.josemodi97.sageactive4j`](https://plugins.gradle.org/plugin/io.github.josemodi97.sageactive4j) | Gradle plugin with project scaffolding task (`sageactive4jInit`) | Gradle 7+ & 8+ |

---

## Installation

### Maven
Import the BOM to manage all versions cleanly:

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
  <!-- Plain Java -->
  <dependency>
    <groupId>io.github.josemodi97</groupId>
    <artifactId>sageactive4j-core</artifactId>
  </dependency>

  <!-- Or for Spring Boot 3 -->
  <!--
  <dependency>
    <groupId>io.github.josemodi97</groupId>
    <artifactId>sageactive4j-spring-boot3-starter</artifactId>
  </dependency>
  -->
</dependencies>
```

### Gradle
```kotlin
dependencies {
    implementation(platform("io.github.josemodi97:sageactive4j-bom:0.1.0"))
    implementation("io.github.josemodi97:sageactive4j-core")

    // Or for Spring Boot 3:
    // implementation("io.github.josemodi97:sageactive4j-spring-boot3-starter")
}
```

---

## Configuring Environments: Production vs Sandbox

In Sage Active, development sandbox accounts and live production accounts connect to the **same regional API gateways**:
* **France**: `https://api.fr.active.sage.com`
* **Spain & Portugal**: `https://api.es.active.sage.com`
* **Germany**: `https://api.de.active.sage.com`
* **SBC Auth**: `https://sbcauth.sage.fr`

A sandbox profile isolates test companies so that development testing will never alter fiscal financial records.

### 1. Spring Boot (`application.yml`)
```yaml
sageactive4j:
  region: FR # FR, ES, DE, or PT
  subscription-key: ${SAGE_SUBSCRIPTION_KEY}
  organization-id: ${SAGE_ORGANIZATION_ID}
  client-id: ${SAGE_CLIENT_ID}
  client-secret: ${SAGE_CLIENT_SECRET}
  token-store:
    file: /var/lib/myapp/sage-tokens.properties
```

### 2. Environment Variables
You can configure the client entirely without files:
* `SAGEACTIVE4J_REGION` = `FR` (or `ES`, `DE`, `PT`)
* `SAGEACTIVE4J_SUBSCRIPTION_KEY` = `your_subscription_key`
* `SAGEACTIVE4J_ORGANIZATION_ID` = `your_organization_id`
* `SAGEACTIVE4J_ACCESS_TOKEN` = `your_bearer_token` (or `CLIENT_ID` + `CLIENT_SECRET` + `REFRESH_TOKEN`)

Then load directly:
```java
SageActive4jClient client = SageActive4jClient.fromEnvironment();
```

---

## Zero-Code CLI Tool (`sageactive4j`)

The `sageactive4j-cli` fat jar (or native binary) lets you test, explore, query, and operate Sage Active directly from your terminal:

```
   _____                   ___         __  _           __ __   _ 
  / ___/____ _____ ____   /   |  _____/ /_(_)   _____ / // /  (_)
  \__ \/ __ `/ __ `/ _ \ / /| | / ___/ __/ / | / / _ \ / // /_ / / 
 ___/ / /_/ / /_/ /  __// ___ |/ /__/ /_/ /| |/ /  __/__  __// /  
/____/\__,_/\__, /\___//_/  |_|\___/\__/_/ |___/\___/  /_/ _/ /   
           /____/                                         /___/   
```

### CLI Command Reference

| Command | Description | Example |
|---|---|---|
| `sageactive4j init` | Interactive onboarding wizard (prompts for region, keys, browser login) | `sageactive4j init` |
| `sageactive4j login` | Desktop browser sign-in via OAuth 2.0 PKCE on loopback port | `sageactive4j login` |
| `sageactive4j logout` | Revokes and cleans up active profile tokens | `sageactive4j logout` |
| `sageactive4j env [profile]` | List profiles or switch active profile | `sageactive4j env sandbox` |
| `sageactive4j test` | Self-test connectivity, user profile, and user access permissions | `sageactive4j test` |
| `sageactive4j org` / `org set <id>` | List available organizations / select active organization | `sageactive4j org set 0000-0000-0000` |
| `sageactive4j customer` | List customers in current organization | `sageactive4j customer --first 10` |
| `sageactive4j account` | List ledger accounts from the chart of accounts | `sageactive4j account` |
| `sageactive4j invoice` | List sales invoices | `sageactive4j invoice` |
| `sageactive4j invoice create` | Create and post a test sales invoice (sandbox profiles) | `sageactive4j invoice create --customer c1 --product p1 --price 50` |
| `sageactive4j quote` | List sales quotes | `sageactive4j quote` |
| `sageactive4j order` | List sales orders | `sageactive4j order` |
| `sageactive4j credit-note` | Generate a credit note for a posted sales invoice | `sageactive4j credit-note --invoice <uuid>` |
| `sageactive4j bank accounts` | List connected bank accounts | `sageactive4j bank accounts` |
| `sageactive4j bank movements` | List imported bank transactions / movements | `sageactive4j bank movements` |
| `sageactive4j bank unreconcile` | Unreconcile a previously linked bank movement | `sageactive4j bank unreconcile --movement <uuid>` |
| `sageactive4j product` | List products and sales tariffs | `sageactive4j product` |
| `sageactive4j tax` | List tax rates and groups | `sageactive4j tax` |
| `sageactive4j file list` | List file attachments | `sageactive4j file list` |
| `sageactive4j file upload` | Upload a file and attach to an entity | `sageactive4j file upload --entity-type CUSTOMER --entity-id <uuid> doc.pdf` |
| `sageactive4j ocr <file>` | Upload receipt/invoice for automated OCR processing | `sageactive4j ocr invoice.pdf` |
| `sageactive4j query "<query>"` | Run an arbitrary GraphQL query or `.graphql` file | `sageactive4j query "{ userProfile { fullName } }"` |

---

## Quickstart: Plain Java

### 1. Instant Client Creation

Create a client in a single line:

```java
import io.github.josemodi97.sageactive4j.Region;
import io.github.josemodi97.sageactive4j.SageActive4jClient;

// Instant creation with token
SageActive4jClient sage = SageActive4jClient.create("SUBSCRIPTION_KEY", "ACCESS_TOKEN", "ORG_ID", Region.FR);

// Or using the fluent builder:
SageActive4jClient sage = SageActive4jClient.builder()
        .region(Region.FR)
        .subscriptionKey("YOUR_SUBSCRIPTION_KEY")
        .clientId("YOUR_CLIENT_ID")
        .clientSecret("YOUR_CLIENT_SECRET")
        .refreshToken("YOUR_REFRESH_TOKEN")
        .organizationId("YOUR_ORGANIZATION_ID")
        .buildClient();
```

### 2. Common Operations

```java
// 1. User Profile & Organizations
System.out.println("Signed in as: " + sage.users().getProfile().getFullName());
for (Organization org : sage.organizations().listUsable()) {
    System.out.println("Org: " + org.getSocialName() + " (" + org.getId() + ")");
}

// 2. Query Customers (Lazy Pagination)
for (Customer customer : sage.thirdParties().allCustomers(ListOptions.first(100))) {
    System.out.println(customer.getCode() + ": " + customer.getSocialName());
}

// 3. Create, Close and Post a Sales Invoice in One Call
InvoicePosting posted = sage.sales().createAndPostInvoice(new SalesInvoiceInput()
        .customerId("CUSTOMER_UUID")
        .documentDate(LocalDate.now())
        .addLine(new SalesInvoiceLineInput()
                .productId("PRODUCT_UUID")
                .totalQuantity(2)
                .unitPrice(new BigDecimal("150.00"))));

System.out.println("Posted Invoice #: " + posted.getOperationalNumber());
System.out.println("Ledger Entry ID: " + posted.getAccountingEntryId());

// 4. Banking: List Accounts and Reconcile
for (BankAccount bank : sage.banks().bankAccounts()) {
    System.out.println("Bank: " + bank.getBankName() + " (" + bank.getIban() + ")");
}

// 5. OCR: Upload Document for Automated Extraction
String fileId = sage.files().upload(new FileAttachment()
        .entityType(FileEntityType.AP_AUTOMATION)
        .file(FileUpload.of(Paths.get("receipt.pdf"))));
System.out.println("Uploaded OCR File ID: " + fileId);

// 6. Raw GraphQL query when needed
Map<String, Object> data = sage.query("{ taxes(first: 5) { nodes { id name percentage } } }");
```

---

## Quickstart: Spring Boot 3 & 2

### 1. Add Dependency
For Spring Boot 3 (`jakarta.*`):
```xml
<dependency>
  <groupId>io.github.josemodi97</groupId>
  <artifactId>sageactive4j-spring-boot3-starter</artifactId>
</dependency>
```

For Spring Boot 2.7 (`javax.*`):
```xml
<dependency>
  <groupId>io.github.josemodi97</groupId>
  <artifactId>sageactive4j-spring-boot2-starter</artifactId>
</dependency>
```

### 2. Inject Client into Controllers
```java
package com.example.demo;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.model.Customer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/sage")
public class SageController {

    private final SageActive4jClient sage;

    public SageController(SageActive4jClient sage) {
        this.sage = sage;
    }

    @GetMapping("/customers")
    public List<Customer> getCustomers() {
        return sage.thirdParties().customers(ListOptions.first(50)).getNodes();
    }

    @GetMapping("/accounts")
    public Object getAccounts() {
        return sage.accounting().accounts(ListOptions.first(50)).getNodes();
    }
}
```

---

## Quickstart: Jakarta & javax Servlet

Add `sageactive4j-jakarta` or `sageactive4j-servlet` for standalone Java EE/Jakarta EE web applications:

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

---

## Scaffold a Starter Project (Maven & Gradle Plugins)

To scaffold a complete, working example tailored to your current project's framework, run:

### Maven
```bash
mvn io.github.josemodi97:sageactive4j-maven-plugin:0.1.0:init
```

### Gradle
Apply `id("io.github.josemodi97.sageactive4j") version "0.1.0"` in your `build.gradle.kts` and run:
```bash
./gradlew sageactive4jInit
```

The plugin inspects your build dependencies and generates:
* **Plain Java**: `sageactive4j/SageActiveExample.java` (main method connecting and printing profile).
* **Servlet**: `sageactive4j/SageActiveServlet.java` (handles `/sage/login` and `/sage/callback`).
* **Spring Boot 2 / 3**: `sageactive4j/SageController.java` (`/sage/accounting/accounts` and `/sage/sales/invoice`) plus sample `sageactive4j.properties`.

---

## Core Architecture & Services

The SDK exposes 11 domain clients through `SageActive4jClient`:

```
SageActive4jClient
 ├── .auth()           - OAuth 2.0 PKCE, token refresh, and token revocation
 ├── .organizations()  - Multi-tenant organization list, metadata, currencies
 ├── .users()          - User profile, user list, and permission policy checks
 ├── .accounting()     - Chart of accounts, fiscal exercises, journal entries, taxes
 ├── .sales()          - Sales invoices, quotes, orders, credit notes, open item settlement
 ├── .purchases()      - Supplier invoices, closing, and settlement
 ├── .banks()          - Bank accounts, bank transactions/movements, reconcile/unreconcile
 ├── .thirdParties()   - Customers and suppliers with multi-address & contact management
 ├── .products()       - Products, sales tariffs, and dynamic tariff price resolution
 ├── .files()          - GraphQL multipart file uploads, attachments, and bulk export
 ├── .catalog()        - Dynamic multidimensional analytics cubes
 └── .localization()   - Localized business error resolution
```

---

## Java Compatibility Matrix

| Runtime / Framework | Compatibility | Engine / Implementation |
|---|---|---|
| **Java 8 (LTS)** | Full Support | `HttpURLConnection`, Java 8 bytecode |
| **Java 11 (LTS)** | Full Support | High-performance `java.net.http.HttpClient` (HTTP/2) |
| **Java 17 (LTS)** | Full Support | High-performance `java.net.http.HttpClient` |
| **Java 21+ (LTS)** | Full Support | High-performance `java.net.http.HttpClient` |
| **GraalVM Native Image**| Full Support | Native compilation with included reflection/resource config |
| **Spring Boot 2.7.x** | Full Support | `sageactive4j-spring-boot2-starter` (`javax.servlet`) |
| **Spring Boot 3.x** | Full Support | `sageactive4j-spring-boot3-starter` (`jakarta.servlet`) |

---

## Sponsorship

If `sageactive4j` saves you time or powers your business, please consider supporting ongoing maintenance and updates:

[![Sponsor via Pesapal](https://img.shields.io/badge/%E2%9D%A4%EF%B8%8F_Sponsor_via-Pesapal-0099ff?style=for-the-badge&logo=heart&logoColor=white)](https://store.pesapal.com/opensourcesponsorship)

---

## License

MIT © [Jose Modi](https://github.com/JoseModi97)
