package com.ecommerce.orderservice.client;

import com.ecommerce.common.dto.ProductDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ProductServiceClient {

    private final WebClient webClient;

    public ProductServiceClient() {
        this.webClient = WebClient.create("http://localhost:8082");
    }

    public ProductDTO getProduct(Long id) {
        return webClient.get()
                .uri("/api/products/{id}", id)
                .retrieve()
                .bodyToMono(ProductDTO.class)
                .block();
    }
}
