package com.ecommerce.orderservice.client;

import com.ecommerce.common.dto.CartItemDTO;
import com.ecommerce.common.security.RequestTokenUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class CartServiceClient {

    private final WebClient webClient;

    public CartServiceClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://cart-service").build();
    }

    private WebClient.RequestHeadersSpec<?> withToken(WebClient.RequestHeadersSpec<?> spec) {
        String token = RequestTokenUtils.getBearerToken();
        return token != null ? spec.header("Authorization", token) : spec;
    }

    public List<CartItemDTO> getCart(Long userId) {
        return withToken(webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/cart")
                        .queryParam("userId", userId)
                        .build()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CartItemDTO>>() {})
                .block();
    }

    public void clearCart(Long userId) {
        withToken(webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/api/cart/clear")
                        .queryParam("userId", userId)
                        .build()))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
