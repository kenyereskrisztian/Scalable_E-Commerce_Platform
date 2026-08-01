package com.ecommerce.orderservice.service.impl;

import com.ecommerce.common.dto.CartItemDTO;
import com.ecommerce.common.dto.ProductDTO;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.InsufficientStockException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.client.CartServiceClient;
import com.ecommerce.orderservice.client.ProductServiceClient;
import com.ecommerce.orderservice.client.UserServiceClient;
import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.domain.Order.OrderStatus;
import com.ecommerce.orderservice.domain.OrderItem;
import com.ecommerce.orderservice.dto.PlaceOrderRequest;
import com.ecommerce.orderservice.repository.OrderItemRepository;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserServiceClient userServiceClient;
    private final CartServiceClient cartServiceClient;
    private final ProductServiceClient productServiceClient;

    @Override
    public Order placeOrder(Long userId, PlaceOrderRequest request) {
        userServiceClient.getUser(userId);

        List<CartItemDTO> cartItems = cartServiceClient.getCart(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .shippingCity(request.getShippingCity())
                .shippingState(request.getShippingState())
                .shippingZipCode(request.getShippingZipCode())
                .notes(request.getNotes())
                .build();

        Order savedOrder = orderRepository.save(order);

        for (CartItemDTO cartItem : cartItems) {
            ProductDTO product = productServiceClient.getProduct(cartItem.getProductId());

            if (product.getStock() < cartItem.getQuantity()) {
                throw new InsufficientStockException(product.getName(), product.getStock(), cartItem.getQuantity());
            }

            BigDecimal itemTotalPrice = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemTotalPrice);

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .productId(cartItem.getProductId())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(itemTotalPrice)
                    .build();

            orderItemRepository.save(orderItem);
            savedOrder.getOrderItems().add(orderItem);
        }

        savedOrder.setTotalAmount(totalAmount);
        Order finalOrder = orderRepository.save(savedOrder);

        cartServiceClient.clearCart(userId);

        return finalOrder;
    }

    @Override
    public List<Order> getOrdersByUser(Long userId) {
        userServiceClient.getUser(userId);
        return orderRepository.findByUserId(userId);
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    @Override
    public Order updateStatus(Long id, OrderStatus status) {
        Order order = getOrderById(id);
        order.setStatus(status);
        return orderRepository.save(order);
    }
}
