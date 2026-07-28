package com.ecommerce.orderservice.client;

import com.ecommerce.common.dto.CartItemDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class CartServiceClient {

    private final WebClient webClient;

    public CartServiceClient() {
        this.webClient = WebClient.create("http://localhost:8083");
    }

    public List<CartItemDTO> getCart(Long userId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/cart")
                        .queryParam("userId", userId)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CartItemDTO>>() {})
                .block();
    }

    public void clearCart(Long userId) {
        webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/api/cart/clear")
                        .queryParam("userId", userId)
                        .build())
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
