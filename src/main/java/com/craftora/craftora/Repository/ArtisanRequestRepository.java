package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.ArtisanRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtisanRequestRepository
        extends JpaRepository<ArtisanRequest, Long> {

    List<ArtisanRequest> findByStatus(String status);

    List<ArtisanRequest> findByUserId(Long userId);
}