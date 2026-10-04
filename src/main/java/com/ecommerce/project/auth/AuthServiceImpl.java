package com.ecommerce.project.auth;

import com.ecommerce.project.security.services.RefreshTokenService;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.notification.email.event.OnPasswordChangedEvent;
import com.ecommerce.project.notification.email.event.OnPasswordResetRequestedEvent;
import com.ecommerce.project.notification.email.event.OnUserRegisteredEvent;
import com.ecommerce.project.security.dto.*;
import com.ecommerce.project.security.jwt.JwtUtils;
import com.ecommerce.project.security.services.UserDetailsImpl;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.ecommerce.project.auth.dto.ForgotPasswordRequestDTO;
import com.ecommerce.project.auth.dto.PasswordChangeRequestDTO;
import com.ecommerce.project.auth.dto.SellerSummaryDTO;
import com.ecommerce.project.auth.dto.UserDTO;
import com.ecommerce.project.auth.dto.UserResponse;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserVerificationTokenRepository userVerificationTokenRepository;
    private final PasswordEncoder encoder;
    private final RoleRepository roleRepository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RefreshTokenService refreshTokenService;

    @Override
    public ResponseEntity<Map<String, String>> forgotPassword(ForgotPasswordRequestDTO forgotPasswordRequestDTO) {

        Optional<User> userOpt = userRepository.findByEmail(forgotPasswordRequestDTO.email());
        if(userOpt.isPresent()) {
            User user = userOpt.get();
            if(user.isEnabled())
                eventPublisher.publishEvent(new OnPasswordResetRequestedEvent(this, user));
        }

        return ResponseEntity.ok(Map.of("message", "If account exists with this email, a password reset link has been sent."));
    }

    @Override
    public void saveVerificationTokenForUser(User user, String token){
        // One token per user (unique user_id): re-sending verification (e.g. on an unverified login)
        // refreshes the existing row rather than inserting a second one.
        UserVerificationToken userVerificationToken = userVerificationTokenRepository.findByUser_UserId(user.getUserId())
                .map(existing -> {
                    existing.setToken(token);
                    existing.setExpiryDate(LocalDateTime.now().plusMinutes(AppConstants.VERIFICATION_EXPIRATION_MINUTES));
                    return existing;
                })
                .orElseGet(() -> new UserVerificationToken(token, user));
        userVerificationTokenRepository.save(userVerificationToken);
    }

    @Override
    public void changePassword(JwtPrincipal userDetails, PasswordChangeRequestDTO requestPasswordChangeRequestDTO) {

        if(userDetails == null) throw new AuthenticationCredentialsNotFoundException("User not logged in");

        User user = userRepository.findByUsername(userDetails.userName()).orElseThrow(() -> new UsernameNotFoundException("Error : Username not found!!!"));

        if(!encoder.matches(requestPasswordChangeRequestDTO.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        if(encoder.matches(requestPasswordChangeRequestDTO.newPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different form current password");
        }

        user.setPassword(encoder.encode(requestPasswordChangeRequestDTO.newPassword()));
        userRepository.save(user);
        // Sign out every session, including this one - the client logs in again with the new
        // password (the frontend's change-password flow does this).
        refreshTokenService.revokeAllForUser(user.getUserId());
        eventPublisher.publishEvent(new OnPasswordChangedEvent(this, user));
    }

    @Override
    public ResponseEntity<MessageResponse> validateEmailVerificationToken(String token){
        Optional<UserVerificationToken> optionalUserVerificationToken = userVerificationTokenRepository.findByToken(token);

        if(optionalUserVerificationToken.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Error: Token not found!!"));
        UserVerificationToken userVerificationToken = optionalUserVerificationToken.get();
        if(userVerificationToken.isExpired())
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(new MessageResponse("Error: Token Expired!!"));

        User user = userVerificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        userVerificationTokenRepository.delete(userVerificationToken);

        return ResponseEntity.ok(new MessageResponse("Token validated successfully!"));
    }

    @Override
    public UserInfoResponse getCurrentUserDetails(Authentication authentication) {
        // /api/auth/** is permitAll, so an anonymous caller reaches here - 401, not an NPE/500.
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal userDetails)) {
            throw new AuthenticationCredentialsNotFoundException("User not logged in");
        }
        List<String> roles = userDetails.authorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());
        return new UserInfoResponse(userDetails.userId(), userDetails.email(), userDetails.userName(), roles);
    }

    @Override
    public UserResponse getAllSellers(Pageable pageDetails) {
        Page<User> allSellerUsers = userRepository.findByRoleName(AppRole.ROLE_SELLER, pageDetails);

        List<SellerSummaryDTO> userDTOS = allSellerUsers.getContent().stream()
            .map(user -> new SellerSummaryDTO(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getName()))
                .collect(Collectors.toList());

        UserResponse userResponse = new UserResponse(
                userDTOS,
                allSellerUsers.getNumber(),
                allSellerUsers.getSize(),
                allSellerUsers.getTotalElements(),
                allSellerUsers.getTotalPages(),
                allSellerUsers.isLast());

        return userResponse;
    }
}
