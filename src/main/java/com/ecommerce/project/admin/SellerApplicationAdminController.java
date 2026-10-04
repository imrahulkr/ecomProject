package com.ecommerce.project.admin;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.seller.SellerApplicationService;
import com.ecommerce.project.seller.SellerApplicationStatus;
import com.ecommerce.project.seller.dto.RejectSellerApplicationRequest;
import com.ecommerce.project.seller.dto.SellerApplicationDTO;
import com.ecommerce.project.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Separate controller from the applicant-facing one, not an "if admin" branch inside it - this
// is the pattern the rest of the admin surface (Phase 6) follows too.
@RestController
@RequestMapping("/api/admin/seller-applications")
@RequiredArgsConstructor
public class SellerApplicationAdminController {

    private final SellerApplicationService sellerApplicationService;
    private final AuthUtil authUtil;

    @GetMapping
    public Page<SellerApplicationDTO> list(
            @RequestParam(name = "status", defaultValue = "PENDING") SellerApplicationStatus status,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return sellerApplicationService.getApplicationsByStatus(status, pageable);
    }

    @PutMapping("/{applicationId}/approve")
    public SellerApplicationDTO approve(@PathVariable Long applicationId) {
        return sellerApplicationService.approve(applicationId, authUtil.loggedInUserId());
    }

    @PutMapping("/{applicationId}/reject")
    public SellerApplicationDTO reject(@PathVariable Long applicationId,
                                        @Valid @RequestBody RejectSellerApplicationRequest request) {
        return sellerApplicationService.reject(applicationId, authUtil.loggedInUserId(), request.reason());
    }
}
