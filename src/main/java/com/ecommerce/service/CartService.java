package com.ecommerce.service;

import com.ecommerce.domain.CartItem;

import java.util.List;

public interface CartService {
    List<CartItem> getCart(Long userId);
    CartItem addItem(Long userId, Long productId, int quantity);
    CartItem updateQuantity(Long cartItemId, int quantity);
    void removeItem(Long cartItemId);
    void clearCart(Long userId);
}
