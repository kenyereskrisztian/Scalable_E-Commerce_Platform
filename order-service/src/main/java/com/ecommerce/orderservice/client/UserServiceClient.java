package com.ecommerce.orderservice.client;

import com.ecommerce.common.dto.UserDTO;
import com.ecommerce.common.security.RequestTokenUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class UserServiceClient {

    private final WebClient webClient;

    public UserServiceClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://user-service").build();
    }

    public UserDTO getUser(Long id) {
        WebClient.RequestHeadersSpec<?> spec = webClient.get()
                .uri("/api/users/{id}", id);
        String token = RequestTokenUtils.getBearerToken();
        if (token != null) {
            spec = spec.header("Authorization", token);
        }
        return spec.retrieve()
                .bodyToMono(UserDTO.class)
                .block();
    }
}
