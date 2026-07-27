package com.ecommerce.service;

import com.ecommerce.domain.Order;
import com.ecommerce.domain.Order.OrderStatus;
import com.ecommerce.dto.PlaceOrderRequest;

import java.util.List;

public interface OrderService {
    Order placeOrder(Long userId, PlaceOrderRequest request);
    List<Order> getOrdersByUser(Long userId);
    Order getOrderById(Long id);
    Order updateStatus(Long id, OrderStatus status);
}
