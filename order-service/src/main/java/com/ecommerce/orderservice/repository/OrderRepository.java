package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.domain.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = "orderItems")
    List<Order> findByUserId(Long userId);

    @Override
    @EntityGraph(attributePaths = "orderItems")
    Optional<Order> findById(Long id);
}
