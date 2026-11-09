package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewReportRepository
        extends JpaRepository<ReviewReport, Long> {

    boolean existsByRatingRatingId(Long ratingId);

    Optional<ReviewReport> findByRatingRatingId(Long ratingId);

    List<ReviewReport> findAllByOrderByCreatedAtDesc();

    List<ReviewReport> findByStatusOrderByCreatedAtDesc(String status);
}