package com.ecommerce.test.service;

import com.ecommerce.domain.CartItem;
import com.ecommerce.domain.Product;
import com.ecommerce.domain.User;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.impl.CartServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    @Test
    void addItem_shouldCreateNewCartItem() {
        User user = User.builder().id(1L).build();
        Product product = Product.builder().id(1L).stock(10).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProductId(1L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItem result = cartService.addItem(1L, 1L, 2);

        assertThat(result.getQuantity()).isEqualTo(2);
    }

    @Test
    void addItem_shouldIncreaseQuantity_whenAlreadyInCart() {
        User user = User.builder().id(1L).build();
        Product product = Product.builder().id(1L).stock(10).build();
        CartItem existing = CartItem.builder().id(1L).user(user).product(product).quantity(1).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProductId(1L, 1L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItem result = cartService.addItem(1L, 1L, 3);

        assertThat(result.getQuantity()).isEqualTo(4);
    }

    @Test
    void addItem_shouldThrow_whenQuantityIsZero() {
        assertThatThrownBy(() -> cartService.addItem(1L, 1L, 0))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateQuantity_shouldThrow_whenCartItemNotFound() {
        when(cartItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateQuantity(99L, 2))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
