package io.github.josemodi97.sageactive4j.spring.boot2;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.auth.FileTokenStore;
import io.github.josemodi97.sageactive4j.auth.InMemoryTokenStore;
import io.github.josemodi97.sageactive4j.auth.TokenStore;
import java.nio.file.Paths;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configures a {@link SageActive4jClient} (closed on shutdown) and its
 * {@link TokenStore} from {@code sageactive4j.*}, plus an actuator health
 * component and, opt-in, browser sign-in endpoints.
 *
 * <p>Only activates once {@code sageactive4j.subscription-key} is set, so an
 * unconfigured app (or a test slice) starts normally with the starter on the
 * classpath. Every bean backs off if you define your own.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SageActive4jProperties.class)
@ConditionalOnClass(SageActive4jClient.class)
@ConditionalOnProperty(prefix = "sageactive4j", name = "subscription-key")
public class SageActive4jAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TokenStore sageActive4jTokenStore(SageActive4jProperties properties) {
        String file = properties.getTokenStore().getFile();
        return file == null || file.trim().isEmpty() ? new InMemoryTokenStore() : new FileTokenStore(Paths.get(file.trim()));
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public SageActive4jClient sageActive4jClient(SageActive4jProperties properties, TokenStore tokenStore) {
        return new SageActive4jClient(properties.toConfig(tokenStore));
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    @ConditionalOnProperty(prefix = "sageactive4j.health", name = "enabled", matchIfMissing = true)
    static class HealthConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "sageActive4jHealthIndicator")
        public SageActive4jHealthIndicator sageActive4jHealthIndicator(SageActive4jClient client,
                                                                       SageActive4jProperties properties) {
            return new SageActive4jHealthIndicator(client, properties.getHealth().getCacheTtl());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = {"org.springframework.web.bind.annotation.GetMapping",
        "io.github.josemodi97.sageactive4j.servlet.SageOAuthFlow"})
    @ConditionalOnProperty(prefix = "sageactive4j.oauth", name = "enabled", havingValue = "true")
    static class OAuthConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public SageActive4jOAuthController sageActive4jOAuthController(SageActive4jClient client,
                                                                       SageActive4jProperties properties,
                                                                       ApplicationEventPublisher events) {
            return new SageActive4jOAuthController(client, properties, events);
        }
    }
}
