package com.pedidos360.catalogo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Igual que en pedidos-service: valida el JWT emitido por Entra ID
 * (issuer, audience, firma, vigencia) antes de dejar pasar cualquier
 * peticion a /api/**.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // OJO: este audience es del microservicio de CATALOGO, es un
    // App Registration DISTINTO al de pedidos-service en Entra ID.
    private static final String EXPECTED_AUDIENCE = "api://CAMBIA-ESTO-POR-TU-CLIENT-ID-CATALOGO";

    private final String issuerUri;

    public SecurityConfig(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
            String issuerUri) {
        this.issuerUri = issuerUri;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()

                // =====================================================================
                // TEMPORAL: igual que en pedidos-service, dejamos abierto mientras se
                // termina de configurar Entra ID. ANTES DE ENTREGAR, cambiar a .authenticated()
                .requestMatchers("/api/**").permitAll()
                // .requestMatchers("/api/**").authenticated()
                // =====================================================================

                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
            .addFilterBefore((request, response, chain) -> chain.doFilter(request, response),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

        OAuth2TokenValidator<Jwt> validadorDeAudience = token -> {
            List<String> audiences = token.getAudience();
            if (audiences != null && audiences.contains(EXPECTED_AUDIENCE)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "El token no tiene el audience esperado", null)
            );
        };

        OAuth2TokenValidator<Jwt> validadorCompleto =
                JwtValidators.createDefaultWithValidators(validadorDeAudience);

        decoder.setJwtValidator(validadorCompleto);
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
