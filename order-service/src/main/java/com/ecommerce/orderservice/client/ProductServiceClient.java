package com.ecommerce.orderservice.client;

import com.ecommerce.common.dto.ProductDTO;
import com.ecommerce.common.security.RequestTokenUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ProductServiceClient {

    private final WebClient webClient;

    public ProductServiceClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://product-service").build();
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
