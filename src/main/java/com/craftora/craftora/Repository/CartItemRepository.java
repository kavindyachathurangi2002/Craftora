package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.CartItem;
import com.craftora.craftora.Entity.Product;
import com.craftora.craftora.Entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserOrderByCreatedAtAsc(User user);

    Optional<CartItem> findByUserAndProduct(User user, Product product);

    void deleteByUser(User user);
}
