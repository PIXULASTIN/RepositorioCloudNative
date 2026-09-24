package com.pedidos360.backend.client;

import com.pedidos360.backend.dto.ProductoDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Llama a catalogo-service reenviando el mismo JWT del usuario que hizo la
 * peticion (ambos servicios validan el mismo token de Entra ID).
 */
@Component
public class CatalogoClient {

    private final WebClient webClient;

    public CatalogoClient(WebClient catalogoWebClient) {
        this.webClient = catalogoWebClient;
    }

    public ProductoDto obtenerProducto(Long id) {
        try {
            return webClient.get()
                    .uri("/api/catalog/products/{id}", id)
                    .headers(h -> h.setBearerAuth(tokenActual()))
                    .retrieve()
                    .bodyToMono(ProductoDto.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw traducir(e, "Producto " + id);
        }
    }

    public void descontarStock(Long id, int cantidad) {
        try {
            webClient.post()
                    .uri(u -> u.path("/api/catalog/products/{id}/descontar-stock")
                            .queryParam("cantidad", cantidad)
                            .build(id))
                    .headers(h -> h.setBearerAuth(tokenActual()))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            throw traducir(e, "Descuento de stock del producto " + id);
        }
    }

    private RuntimeException traducir(WebClientResponseException e, String contexto) {
        String detalle = contexto + ": catalogo respondio " + e.getStatusCode().value();
        if (e.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT)) {
            return new IllegalStateException(detalle + " (stock insuficiente)");
        }
        return new IllegalArgumentException(detalle);
    }

    private String tokenActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }
        throw new IllegalStateException("No hay token de usuario para llamar al catalogo");
    }
}
