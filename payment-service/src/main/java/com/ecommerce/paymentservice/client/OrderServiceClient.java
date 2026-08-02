package com.ecommerce.paymentservice.client;

import com.ecommerce.common.dto.OrderDTO;
import com.ecommerce.common.security.RequestTokenUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OrderServiceClient {

    private final WebClient webClient;

    public OrderServiceClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://order-service").build();
    }

    public OrderDTO getOrder(Long id) {
        WebClient.RequestHeadersSpec<?> spec = webClient.get()
                .uri("/api/orders/{id}", id);
        String token = RequestTokenUtils.getBearerToken();
        if (token != null) {
            spec = spec.header("Authorization", token);
        }
        return spec.retrieve()
                .bodyToMono(OrderDTO.class)
                .block();
    }
}
