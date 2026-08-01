package com.ecommerce.cartservice.client;

import com.ecommerce.common.dto.ProductDTO;
import com.ecommerce.common.security.RequestTokenUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ProductServiceClient {

    private final WebClient webClient;

    public ProductServiceClient() {
        this.webClient = WebClient.create("http://localhost:8082");
    }

    public ProductDTO getProduct(Long id) {
        WebClient.RequestHeadersSpec<?> spec = webClient.get()
                .uri("/api/products/{id}", id);
        String token = RequestTokenUtils.getBearerToken();
        if (token != null) {
            spec = spec.header("Authorization", token);
        }
        return spec.retrieve()
                .bodyToMono(ProductDTO.class)
                .block();
    }
}
