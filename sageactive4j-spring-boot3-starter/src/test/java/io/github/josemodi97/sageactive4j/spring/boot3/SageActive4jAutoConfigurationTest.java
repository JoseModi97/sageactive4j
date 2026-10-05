package io.github.josemodi97.sageactive4j.spring.boot3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.josemodi97.sageactive4j.Region;
import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.SageActive4jConfig;
import io.github.josemodi97.sageactive4j.auth.FileTokenStore;
import io.github.josemodi97.sageactive4j.auth.InMemoryTokenStore;
import io.github.josemodi97.sageactive4j.auth.TokenStore;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class SageActive4jAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SageActive4jAutoConfiguration.class));

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SageActive4jAutoConfiguration.class));

    @TempDir
    Path dir;

    @Test
    void staysOffWithoutASubscriptionKey() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(SageActive4jClient.class);
            assertThat(context).doesNotHaveBean(TokenStore.class);
        });
    }

    @Test
    void bindsEveryCoreSetting() {
        runner.withPropertyValues(
                "sageactive4j.subscription-key=sub",
                "sageactive4j.region=de",
                "sageactive4j.organization-id=org-1",
                "sageactive4j.client-id=cid",
                "sageactive4j.client-secret=secret",
                "sageactive4j.refresh-token=rt",
                "sageactive4j.read-timeout=5s",
                "sageactive4j.connect-timeout=1500ms",
                "sageactive4j.max-retry-attempts=0",
                "sageactive4j.max-concurrent-mutations=4").run(context -> {
                    SageActive4jConfig config = context.getBean(SageActive4jClient.class).getConfig();
                    assertThat(config.getRegion()).isEqualTo(Region.DE);
                    assertThat(config.getOrganizationId()).isEqualTo("org-1");
                    assertThat(config.getClientSecret()).isEqualTo("secret");
                    assertThat(config.getReadTimeoutMillis()).isEqualTo(5000);
                    assertThat(config.getConnectTimeoutMillis()).isEqualTo(1500);
                    assertThat(config.getMaxRetryAttempts()).isZero();
                    assertThat(config.getMaxConcurrentMutations()).isEqualTo(4);
                    assertThat(context.getBean(TokenStore.class)).isInstanceOf(InMemoryTokenStore.class);
                    assertThat(context.getBean(SageActive4jProperties.class).toString()).doesNotContain("secret");
                });
    }

    @Test
    void fileTokenStoreFromProperties() {
        Path file = dir.resolve("tokens.properties");
        runner.withPropertyValues("sageactive4j.subscription-key=sub", "sageactive4j.token-store.file=" + file)
                .run(context -> {
                    TokenStore store = context.getBean(TokenStore.class);
                    assertThat(store).isInstanceOf(FileTokenStore.class);
                    assertThat(((FileTokenStore) store).getFile()).isEqualTo(file);
                    assertThat(context.getBean(SageActive4jClient.class).getConfig().getTokenStore()).isSameAs(store);
                });
    }

    @Test
    void yourOwnTokenStoreWins() {
        runner.withPropertyValues("sageactive4j.subscription-key=sub")
                .withUserConfiguration(CustomStore.class)
                .run(context -> {
                    assertThat(context.getBean(TokenStore.class)).isSameAs(CustomStore.STORE);
                    assertThat(context.getBean(SageActive4jClient.class).getConfig().getTokenStore()).isSameAs(CustomStore.STORE);
                });
    }

    @Test
    void yourOwnClientWins() {
        runner.withPropertyValues("sageactive4j.subscription-key=sub")
                .withUserConfiguration(CustomClient.class)
                .run(context -> assertThat(context.getBean(SageActive4jClient.class)).isSameAs(CustomClient.CLIENT));
    }

    @Test
    void theClientIsClosedWithTheContext() {
        AtomicReference<SageActive4jClient> client = new AtomicReference<>();
        runner.withPropertyValues("sageactive4j.subscription-key=sub")
                .run(context -> client.set(context.getBean(SageActive4jClient.class)));
        assertThatThrownBy(() -> client.get().async(c -> 1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void healthIndicatorWithActuatorUnlessDisabled() {
        runner.withPropertyValues("sageactive4j.subscription-key=sub")
                .run(context -> assertThat(context).hasSingleBean(SageActive4jHealthIndicator.class));
        runner.withPropertyValues("sageactive4j.subscription-key=sub", "sageactive4j.health.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(SageActive4jHealthIndicator.class));
        runner.withPropertyValues("sageactive4j.subscription-key=sub")
                .withClassLoader(new FilteredClassLoader("org.springframework.boot.actuate"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(SageActive4jHealthIndicator.class);
                });
    }

    @Test
    void signInEndpointsAreOptIn() {
        webRunner.withPropertyValues("sageactive4j.subscription-key=sub", "sageactive4j.client-id=cid",
                        "sageactive4j.redirect-uri=https://app/sage/oauth/callback")
                .run(context -> assertThat(context).doesNotHaveBean(SageActive4jOAuthController.class));
        webRunner.withPropertyValues("sageactive4j.subscription-key=sub", "sageactive4j.client-id=cid",
                        "sageactive4j.redirect-uri=https://app/sage/oauth/callback", "sageactive4j.oauth.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(SageActive4jOAuthController.class));
    }

    @Test
    void signInEndpointsNeverRegisterOutsideAServletApp() {
        runner.withPropertyValues("sageactive4j.subscription-key=sub", "sageactive4j.client-id=cid",
                        "sageactive4j.redirect-uri=https://app/cb", "sageactive4j.oauth.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(SageActive4jOAuthController.class));
    }

    @Test
    void signInWithoutARedirectUriFailsAtStartupWithAClearMessage() {
        webRunner.withPropertyValues("sageactive4j.subscription-key=sub", "sageactive4j.client-id=cid",
                        "sageactive4j.oauth.enabled=true")
                .run(context -> assertThat(context).getFailure()
                        .hasStackTraceContaining("sageactive4j.redirect-uri"));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomStore {
        static final TokenStore STORE = new InMemoryTokenStore();

        @Bean
        TokenStore myStore() {
            return STORE;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomClient {
        static final SageActive4jClient CLIENT = new SageActive4jClient(SageActive4jConfig.builder()
                .subscriptionKey("mine").build());

        @Bean
        SageActive4jClient myClient() {
            return CLIENT;
        }
    }
}
