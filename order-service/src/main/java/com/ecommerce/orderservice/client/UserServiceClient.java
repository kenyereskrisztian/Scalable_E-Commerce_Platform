package com.ecommerce.orderservice.client;

import com.ecommerce.common.dto.UserDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class UserServiceClient {

    private final WebClient webClient;

    public UserServiceClient() {
        this.webClient = WebClient.create("http://localhost:8081");
    }

    public UserDTO getUser(Long id) {
        return webClient.get()
                .uri("/api/users/{id}", id)
                .retrieve()
                .bodyToMono(UserDTO.class)
                .block();
    }
}
