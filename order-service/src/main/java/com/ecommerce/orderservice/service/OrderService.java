package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.domain.Order.OrderStatus;
import com.ecommerce.orderservice.dto.PlaceOrderRequest;

import java.util.List;

public interface OrderService {
    Order placeOrder(Long userId, PlaceOrderRequest request);
    List<Order> getOrdersByUser(Long userId);
    Order getOrderById(Long id);
    Order updateStatus(Long id, OrderStatus status);
}
