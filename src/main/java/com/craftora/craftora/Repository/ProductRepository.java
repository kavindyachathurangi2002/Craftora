package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByArtisan(Artisan artisan);

    long countByArtisan(Artisan artisan);
}