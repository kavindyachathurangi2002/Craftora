package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.Artisan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtisanRepository extends JpaRepository<Artisan, Long> {

    List<Artisan> findByStatusIgnoreCase(String status);

    Optional<Artisan> findByUserUserId(Long userId);
}