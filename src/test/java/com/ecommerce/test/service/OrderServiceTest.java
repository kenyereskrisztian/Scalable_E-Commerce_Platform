package com.ecommerce.test.service;

import com.ecommerce.domain.*;
import com.ecommerce.dto.PlaceOrderRequest;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.OrderItemRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void placeOrder_shouldSucceed() {
        User user = User.builder().id(1L).build();
        Category category = Category.builder().id(1L).name("Test").build();
        Product product = Product.builder().id(1L).name("Item").price(BigDecimal.TEN).stock(5).category(category).build();
        CartItem cartItem = CartItem.builder().user(user).product(product).quantity(2).build();
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setShippingAddress("Addr 1");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(cartItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.placeOrder(1L, request);

        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
    }

    @Test
    void placeOrder_shouldThrow_whenCartEmpty() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.placeOrder(1L, new PlaceOrderRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void placeOrder_shouldThrow_whenInsufficientStock() {
        User user = User.builder().id(1L).build();
        Category category = Category.builder().id(1L).name("Test").build();
        Product product = Product.builder().id(1L).name("Item").price(BigDecimal.TEN).stock(1).category(category).build();
        CartItem cartItem = CartItem.builder().user(user).product(product).quantity(5).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(cartItem));

        assertThatThrownBy(() -> orderService.placeOrder(1L, new PlaceOrderRequest()))
                .isInstanceOf(InsufficientStockException.class);
    }
}
