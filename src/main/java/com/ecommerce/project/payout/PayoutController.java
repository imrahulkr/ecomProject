package com.ecommerce.project.payout;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.payout.dto.EarningsSummaryDTO;
import com.ecommerce.project.payout.dto.LedgerEntryDTO;
import com.ecommerce.project.payout.dto.PayoutDTO;
import com.ecommerce.project.payout.dto.PayoutRequest;
import com.ecommerce.project.payout.dto.SellerBalanceDTO;
import com.ecommerce.project.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// /api/seller/** is ADMIN or SELLER and /api/admin/** is ADMIN only (SecurityConfig). Seller
// endpoints always use the caller's own id - never one from the request.
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PayoutController {

    private final SellerLedgerService ledgerService;
    private final AuthUtil authUtil;

    @GetMapping("/seller/payouts/summary")
    public EarningsSummaryDTO mySummary() {
        return ledgerService.summary(authUtil.loggedInUserId());
    }

    @GetMapping("/seller/payouts/ledger")
    public Page<LedgerEntryDTO> myLedger(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize) {
        return ledgerService.ledger(authUtil.loggedInUserId(), PageRequest.of(pageNumber, pageSize));
    }

    @GetMapping("/seller/payouts")
    public Page<PayoutDTO> myPayouts(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize) {
        return ledgerService.payouts(authUtil.loggedInUserId(), PageRequest.of(pageNumber, pageSize));
    }

    @GetMapping("/admin/payouts/balances")
    public List<SellerBalanceDTO> balances() {
        return ledgerService.balances();
    }

    @GetMapping("/admin/payouts")
    public Page<PayoutDTO> allPayouts(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize) {
        return ledgerService.payouts(null, PageRequest.of(pageNumber, pageSize));
    }

    @PostMapping("/admin/sellers/{sellerId}/payouts")
    public ResponseEntity<PayoutDTO> payout(@PathVariable Long sellerId, @Valid @RequestBody(required = false) PayoutRequest request) {
        PayoutDTO payout = ledgerService.payout(sellerId, authUtil.loggedInUserId(),
                request != null ? request.reference() : null);
        return new ResponseEntity<>(payout, HttpStatus.CREATED);
    }
}
