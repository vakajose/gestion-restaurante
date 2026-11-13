package com.restaurant.app.modules.backoffice.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
class BackofficeSecurityConfig {

    private final PlatformJwtService platformJwtService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    BackofficeSecurityConfig(
        PlatformJwtService platformJwtService,
        CorsConfigurationSource corsConfigurationSource,
        AuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.platformJwtService = platformJwtService;
        this.corsConfigurationSource = corsConfigurationSource;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    @Order(1)
    SecurityFilterChain backofficeSecurityFilterChain(HttpSecurity http) throws Exception {
        PlatformJwtAuthenticationFilter platformFilter = new PlatformJwtAuthenticationFilter(platformJwtService);

        http
            .securityMatcher("/api/v1/backoffice/**", "/actuator/**")
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/backoffice/auth/login",
                    "/api/v1/backoffice/auth/refresh",
                    "/api/v1/backoffice/auth/logout",
                    "/actuator/health",
                    "/actuator/health/**"
                ).permitAll()
                .anyRequest().hasAnyRole("SUPERADMIN", "PLATFORM_OPERATOR")
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
            .addFilterBefore(platformFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
