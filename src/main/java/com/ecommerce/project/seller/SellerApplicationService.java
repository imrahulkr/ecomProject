package com.ecommerce.project.seller;

import com.ecommerce.project.seller.dto.ApplySellerRequest;
import com.ecommerce.project.seller.dto.SellerApplicationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SellerApplicationService {

    SellerApplicationDTO apply(Long userId, ApplySellerRequest request);

    List<SellerApplicationDTO> getMyApplications(Long userId);

    Page<SellerApplicationDTO> getApplicationsByStatus(SellerApplicationStatus status, Pageable pageable);

    SellerApplicationDTO approve(Long applicationId, Long adminUserId);

    SellerApplicationDTO reject(Long applicationId, Long adminUserId, String reason);
}
