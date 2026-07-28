package com.ecommerce.cartservice.service.impl;

import com.ecommerce.cartservice.client.ProductServiceClient;
import com.ecommerce.cartservice.client.UserServiceClient;
import com.ecommerce.cartservice.domain.CartItem;
import com.ecommerce.cartservice.repository.CartItemRepository;
import com.ecommerce.cartservice.service.CartService;
import com.ecommerce.common.exception.BadRequestException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;

    @Override
    public List<CartItem> getCart(Long userId) {
        userServiceClient.getUser(userId);
        return cartItemRepository.findByUserId(userId);
    }

    @Override
    public CartItem addItem(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }

        userServiceClient.getUser(userId);
        productServiceClient.getProduct(productId);

        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndProductId(userId, productId);

        if (existingItem.isPresent()) {
            CartItem cartItem = existingItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            return cartItemRepository.save(cartItem);
        }

        CartItem cartItem = CartItem.builder()
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .build();
        return cartItemRepository.save(cartItem);
    }

    @Override
    public CartItem updateQuantity(Long cartItemId, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new com.ecommerce.common.exception.ResourceNotFoundException("CartItem", cartItemId));

        cartItem.setQuantity(quantity);
        return cartItemRepository.save(cartItem);
    }

    @Override
    public void removeItem(Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new com.ecommerce.common.exception.ResourceNotFoundException("CartItem", cartItemId));
        cartItemRepository.delete(cartItem);
    }

    @Override
    public void clearCart(Long userId) {
        userServiceClient.getUser(userId);
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        cartItemRepository.deleteAll(cartItems);
    }
}
