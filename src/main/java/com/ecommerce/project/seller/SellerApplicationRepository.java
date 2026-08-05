package com.ecommerce.project.seller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {

    List<SellerApplication> findByUser_UserIdOrderByAppliedAtDesc(Long userId);

    Optional<SellerApplication> findFirstByUser_UserIdAndStatus(Long userId, SellerApplicationStatus status);

    Page<SellerApplication> findByStatus(SellerApplicationStatus status, Pageable pageable);
}
