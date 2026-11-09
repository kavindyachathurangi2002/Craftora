package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.ArtisanCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtisanCertificateRepository
        extends JpaRepository<ArtisanCertificate, Long> {

    List<ArtisanCertificate> findByRequestId(Long requestId);

    Optional<ArtisanCertificate> findFirstByFileName(String fileName);
}