package com.pedidos360.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * El Gateway usa "WebFlux" (reactivo) en vez del Spring MVC clasico que usan
 * pedidos-service y catalogo-service, porque Spring Cloud Gateway esta
 * construido sobre esa base. Por eso las clases se llaman "Reactive..." y
 * "ServerHttpSecurity" en vez de "HttpSecurity".
 *
 * La idea es la misma que en los microservicios: validar issuer, audience,
 * firma y vigencia del JWT antes de dejar pasar la peticion.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    // Audience del propio Gateway. Puedes crear un App Registration especifico
    // para el Gateway en Entra ID, o reutilizar el mismo audience de uno de
    // los microservicios si el curso no pide uno separado.
    private static final String EXPECTED_AUDIENCE = "api://CAMBIA-ESTO-POR-TU-CLIENT-ID-GATEWAY";

    private final String issuerUri;

    public GatewaySecurityConfig(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
            String issuerUri) {
        this.issuerUri = issuerUri;
    }

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchange -> exchange
                .pathMatchers("/actuator/health").permitAll()

                // =====================================================================
                // TEMPORAL: igual que en los microservicios, mientras se configura
                // Entra ID. ANTES DE ENTREGAR, cambiar a .authenticated()
                .pathMatchers("/api/**").permitAll()
                // .pathMatchers("/api/**").authenticated()
                // =====================================================================

                .anyExchange().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder())));

        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withIssuerLocation(issuerUri)
                .build();

        OAuth2TokenValidator<Jwt> validadorDeAudience = token -> {
            List<String> audiences = token.getAudience();
            if (audiences != null && audiences.contains(EXPECTED_AUDIENCE)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "El token no tiene el audience esperado", null)
            );
        };

        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(validadorDeAudience));
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
