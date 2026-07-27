package com.ecommerce.test.repository;

import com.ecommerce.domain.User;
import com.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_shouldReturnUser_whenEmailExists() {
        User user = User.builder()
                .email("test@test.com")
                .password("pass")
                .firstName("John")
                .lastName("Doe")
                .active(true)
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenEmailNotExists() {
        Optional<User> found = userRepository.findByEmail("nonexistent@test.com");

        assertThat(found).isEmpty();
    }
}
