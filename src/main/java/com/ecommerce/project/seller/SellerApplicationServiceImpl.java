package com.ecommerce.project.seller;

import com.ecommerce.project.auth.AppRole;
import com.ecommerce.project.auth.Role;
import com.ecommerce.project.auth.RoleRepository;
import com.ecommerce.project.auth.User;
import com.ecommerce.project.auth.UserRepository;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.notification.email.event.OnSellerApplicationApprovedEvent;
import com.ecommerce.project.notification.email.event.OnSellerApplicationRejectedEvent;
import com.ecommerce.project.seller.dto.ApplySellerRequest;
import com.ecommerce.project.seller.dto.SellerApplicationDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerApplicationServiceImpl implements SellerApplicationService {

    private final SellerApplicationRepository sellerApplicationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SellerApplicationDTO apply(Long userId, ApplySellerRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user", "userId", userId));

        boolean alreadySeller = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> role.getRoleName() == AppRole.ROLE_SELLER);
        if (alreadySeller) {
            throw new APIException("You are already an approved seller");
        }

        sellerApplicationRepository.findFirstByUser_UserIdAndStatus(userId, SellerApplicationStatus.PENDING)
                .ifPresent(existing -> {
                    throw new APIException("You already have a pending seller application");
                });

        SellerApplication application = new SellerApplication(
                user, request.businessName(), request.businessDescription());
        application = sellerApplicationRepository.save(application);
        return toDTO(application);
    }

    @Override
    @Transactional
    public List<SellerApplicationDTO> getMyApplications(Long userId) {
        return sellerApplicationRepository.findByUser_UserIdOrderByAppliedAtDesc(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public Page<SellerApplicationDTO> getApplicationsByStatus(SellerApplicationStatus status, Pageable pageable) {
        return sellerApplicationRepository.findByStatus(status, pageable).map(this::toDTO);
    }

    @Override
    @Transactional
    public SellerApplicationDTO approve(Long applicationId, Long adminUserId) {
        SellerApplication application = sellerApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("sellerApplication", "id", applicationId));
        if (application.getStatus() != SellerApplicationStatus.PENDING) {
            throw new APIException("Application has already been decided");
        }

        Role sellerRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER)
                .orElseThrow(() -> new IllegalStateException("ROLE_SELLER is not configured"));

        User user = application.getUser();
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
        user.getRoles().add(sellerRole);
        userRepository.save(user);

        application.setStatus(SellerApplicationStatus.APPROVED);
        application.setDecidedAt(Instant.now());
        application.setDecidedByAdminId(adminUserId);
        application = sellerApplicationRepository.save(application);

        eventPublisher.publishEvent(new OnSellerApplicationApprovedEvent(this, application));
        return toDTO(application);
    }

    @Override
    @Transactional
    public SellerApplicationDTO reject(Long applicationId, Long adminUserId, String reason) {
        SellerApplication application = sellerApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("sellerApplication", "id", applicationId));
        if (application.getStatus() != SellerApplicationStatus.PENDING) {
            throw new APIException("Application has already been decided");
        }

        application.setStatus(SellerApplicationStatus.REJECTED);
        application.setRejectionReason(reason);
        application.setDecidedAt(Instant.now());
        application.setDecidedByAdminId(adminUserId);
        application = sellerApplicationRepository.save(application);

        eventPublisher.publishEvent(new OnSellerApplicationRejectedEvent(this, application));
        return toDTO(application);
    }

    private SellerApplicationDTO toDTO(SellerApplication application) {
        return SellerApplicationDTO.builder()
                .id(application.getId())
                .userId(application.getUser().getUserId())
                .businessName(application.getBusinessName())
                .businessDescription(application.getBusinessDescription())
                .status(application.getStatus())
                .rejectionReason(application.getRejectionReason())
                .appliedAt(application.getAppliedAt())
                .decidedAt(application.getDecidedAt())
                .build();
    }
}
