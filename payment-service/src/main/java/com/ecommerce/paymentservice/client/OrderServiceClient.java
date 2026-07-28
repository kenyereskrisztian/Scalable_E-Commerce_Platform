package com.ecommerce.paymentservice.client;

import com.ecommerce.common.dto.OrderDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OrderServiceClient {

    private final WebClient webClient;

    public OrderServiceClient() {
        this.webClient = WebClient.create("http://localhost:8084");
    }

    public OrderDTO getOrder(Long id) {
        return webClient.get()
                .uri("/api/orders/{id}", id)
                .retrieve()
                .bodyToMono(OrderDTO.class)
                .block();
    }
}
