package com.pedidos360.backend.config;

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
 * Esta clase reemplaza al "filtro" mencionado en el enunciado: Spring Security
 * ya trae un filtro (BearerTokenAuthenticationFilter) que se encarga de leer
 * el header "Authorization: Bearer <token>", validarlo contra Entra ID
 * (firma, expiracion, issuer) y dejarlo disponible en el contexto de seguridad.
 *
 * Lo que TU debes configurar es:
 *  1) De donde viene el JWT a validar (issuer-uri en application.yml)
 *  2) Que audience (aud) es valida (JwtAudienceValidator mas abajo)
 *  3) Que rutas requieren autenticacion
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Debe coincidir EXACTO con el Application ID URI o Client ID
    // que configuraste como "expuesta API" en el App Registration del backend en Entra ID.
    private static final String EXPECTED_AUDIENCE = "api://CAMBIA-ESTO-POR-TU-CLIENT-ID";

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
                // Ejemplo de autorizacion por rol usando los claims del token:
                // .requestMatchers("/api/admin/**").hasAuthority("SCOPE_Admin")

                // =====================================================================
                // TEMPORAL: mientras se termina de configurar el tenant de Entra ID,
                // dejamos las rutas abiertas para poder desarrollar y probar con Postman
                // sin necesidad de un token real. ANTES DE ENTREGAR, vuelve a activar
                // la linea de abajo que exige autenticacion, y borra/comenta esta.
                .requestMatchers("/api/**").permitAll()
                // .requestMatchers("/api/**").authenticated()
                // =====================================================================

                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
            // el filtro de OAuth2 Resource Server ya se registra automaticamente
            // aqui como reemplazo del filtro manual de validacion de JWT
            .addFilterBefore((request, response, chain) -> chain.doFilter(request, response),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

        // Validador extra de audience: sin esto, cualquier token valido de Entra ID
        // (de cualquier app) pasaria la validacion. Con esto, solo pasan los
        // tokens emitidos para ESTA api.
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
