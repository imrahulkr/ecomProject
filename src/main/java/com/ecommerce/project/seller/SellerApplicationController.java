package com.ecommerce.project.seller;

import com.ecommerce.project.seller.dto.ApplySellerRequest;
import com.ecommerce.project.seller.dto.SellerApplicationDTO;
import com.ecommerce.project.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Deliberately not under /api/seller/** - that prefix is gated to users who already hold
// ROLE_SELLER (see SecurityConfig), but applying is exactly what a user without that role yet
// needs to do. Falls through to the default "any authenticated user" rule instead.
@RestController
@RequestMapping("/api/seller-applications")
@RequiredArgsConstructor
public class SellerApplicationController {

    private final SellerApplicationService sellerApplicationService;
    private final AuthUtil authUtil;

    @PostMapping
    public ResponseEntity<SellerApplicationDTO> apply(@Valid @RequestBody ApplySellerRequest request) {
        SellerApplicationDTO dto = sellerApplicationService.apply(authUtil.loggedInUserId(), request);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<List<SellerApplicationDTO>> getMyApplications() {
        return ResponseEntity.ok(sellerApplicationService.getMyApplications(authUtil.loggedInUserId()));
    }
}
