package com.restaurant.app.core.config;

import com.restaurant.app.core.common.DataInitializer;
import com.restaurant.app.core.security.JwtAuthenticationFilter;
import com.restaurant.app.core.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TwelveFactorProfileTests {

    private Properties loadYaml(String filename) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(filename));
        Properties props = factory.getObject();
        assertThat(props).isNotNull();
        return props;
    }

    @Test
    @DisplayName("application-prod.yml debe configurar HikariCP explícito (15/5), Flyway seguro y cero secretos quemados")
    void prodProfileShouldHaveExplicitPoolAndStrictPlaceholders() {
        Properties prod = loadYaml("application-prod.yml");

        assertThat(prod.getProperty("spring.datasource.url")).isEqualTo("${SPRING_DATASOURCE_URL}");
        assertThat(prod.getProperty("spring.datasource.username")).isEqualTo("${SPRING_DATASOURCE_USERNAME}");
        assertThat(prod.getProperty("spring.datasource.password")).isEqualTo("${SPRING_DATASOURCE_PASSWORD}");
        assertThat(prod.getProperty("spring.datasource.hikari.maximum-pool-size")).isEqualTo("15");
        assertThat(prod.getProperty("spring.datasource.hikari.minimum-idle")).isEqualTo("5");
        assertThat(prod.getProperty("spring.flyway.clean-disabled")).isEqualTo("true");
        assertThat(prod.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo("false");
        assertThat(prod.getProperty("restaurant.security.jwt.secret")).isEqualTo("${JWT_SECRET}");
        assertThat(prod.getProperty("app.cors.allowed-origins")).isEqualTo("${CORS_ALLOWED_ORIGINS}");
    }

    @Test
    @DisplayName("application-staging.yml debe configurar HikariCP reducido (5/1) y placeholders estrictos")
    void stagingProfileShouldHaveReducedPoolAndStrictPlaceholders() {
        Properties staging = loadYaml("application-staging.yml");

        assertThat(staging.getProperty("spring.datasource.url")).isEqualTo("${SPRING_DATASOURCE_URL}");
        assertThat(staging.getProperty("spring.datasource.hikari.maximum-pool-size")).isEqualTo("5");
        assertThat(staging.getProperty("spring.datasource.hikari.minimum-idle")).isEqualTo("1");
        assertThat(staging.getProperty("spring.flyway.clean-disabled")).isEqualTo("true");
        assertThat(staging.getProperty("restaurant.security.jwt.secret")).isEqualTo("${JWT_SECRET}");
        assertThat(staging.getProperty("app.cors.allowed-origins")).isEqualTo("${CORS_ALLOWED_ORIGINS}");
    }

    @Test
    @DisplayName("application-testing-local.yml debe apuntar a restaurant_testing sin baseline y exigir JWT_SECRET")
    void testingLocalProfileShouldTargetCleanTestingDb() {
        Properties testingLocal = loadYaml("application-testing-local.yml");

        assertThat(testingLocal.getProperty("spring.datasource.url")).contains("restaurant_testing");
        assertThat(testingLocal.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo("false");
        assertThat(testingLocal.getProperty("restaurant.security.jwt.secret")).isEqualTo("${JWT_SECRET}");
    }

    @Test
    @DisplayName("DataInitializer debe estar restringido a los perfiles dev y test")
    void dataInitializerMustOnlyRunInDevAndTest() {
        Profile profile = DataInitializer.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactlyInAnyOrder("dev", "test");
    }

    @Test
    @DisplayName("SecurityConfig debe restringir CORS a los orígenes externalizados")
    void securityConfigShouldUseExternalizedCorsOrigins() {
        SecurityConfig config = new SecurityConfig(
            mock(JwtAuthenticationFilter.class),
            mock(ObjectMapper.class),
            List.of(" https://app.example.com ", "https://admin.example.com", " ")
        );

        CorsConfiguration cors = config.corsConfigurationSource().getCorsConfiguration(new MockHttpServletRequest());
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns())
            .containsExactly("https://app.example.com", "https://admin.example.com");
    }
}
