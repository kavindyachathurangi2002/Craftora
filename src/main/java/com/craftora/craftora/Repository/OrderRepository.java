package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.Order;
import com.craftora.craftora.Entity.Customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerOrderByOrderDateDesc(Customer customer);
}
