package com.ecommerce.project.controller;

import com.ecommerce.project.config.AppConstants;

import com.ecommerce.project.payload.AuthenticationResult;
import com.ecommerce.project.payload.ForgotPasswordRequestDTO;
import com.ecommerce.project.payload.PasswordChangeRequestDTO;
import com.ecommerce.project.payload.ResetPasswordRequestDTO;

import com.ecommerce.project.security.dto.JwtPrincipal;
import com.ecommerce.project.security.dto.LoginRequest;
import com.ecommerce.project.security.dto.SignupRequest;
import com.ecommerce.project.security.dto.MessageResponse;

import com.ecommerce.project.service.AuthService;
import com.ecommerce.project.service.PasswordResetTokenService;
import com.ecommerce.project.service.RateLimiterService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private  final AuthService authService;
    private  final RateLimiterService rateLimiterService;
    private  final PasswordResetTokenService passwordResetTokenService;

    public AuthController(AuthService authService, RateLimiterService rateLimiterService, PasswordResetTokenService passwordResetTokenService) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
        this.passwordResetTokenService = passwordResetTokenService;
    }

//    @PostMapping("/signin")
//    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
//        AuthenticationResult authenticationResult = authService.login(loginRequest);
//        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, authenticationResult.getJwtCookie().toString()).body(authenticationResult.getResponse());
//    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest){
        return authService.register(signupRequest);
    }

    @GetMapping("/verif-yemail")
    public ResponseEntity<?> verifyUserEmail(@RequestParam("token") String token){
        return authService.validateEmailVerificationToken(token);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO forgotPasswordRequestDTO,
            HttpServletRequest httpRequest
    ){
        String clientIP = httpRequest.getRemoteAddr();
        Bucket bucket = rateLimiterService.resolveBucket("forgot-pw" + clientIP);

        if(!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many requests. Please try again later."));
        }
        return authService.forgotPassword(forgotPasswordRequestDTO);
    }

    @GetMapping("/reset-password/validate")
    public ResponseEntity<?> validatePasswordResetToken(@RequestParam("token") String token){
        String result = passwordResetTokenService.validateToken(token);
        return switch (result) {
            case "VALID" -> ResponseEntity.ok().body(Map.of("valid", true));
            case "EXPIRED" -> ResponseEntity.badRequest().body(Map.of("valid", false, "reason", "This reset link has expired. Kindly request a new one."));
            case "ALREADY_USED" -> ResponseEntity.badRequest().body(Map.of("valid", false, "reason", "This reset link has already been used."));
            default -> ResponseEntity.badRequest().body(Map.of("valid", false, "reason", "Invalid reset link. "));
        };
    }


    @GetMapping("/reset-password")
    public ResponseEntity<?> resetPasswordRequest(@Valid @RequestBody
                                           ResetPasswordRequestDTO request,
                                           HttpServletRequest httpRequest){
        String clientIP = httpRequest.getRemoteAddr();
        Bucket bucket = rateLimiterService.resolveBucket("reset-pw" +  clientIP);
        if(!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many requests. Please try again later."));
        }

        String result = passwordResetTokenService.resetPassword(request.getToken(), request.getNewPassword());
        return switch (result) {
            case "SUCCESS" -> ResponseEntity.ok().body(Map.of("message", "Password reset successfully. You can now log in."));
            case "EXPIRED" -> ResponseEntity.badRequest().body(Map.of("message", "This reset link has expired. Kindly request a new one."));
            case "ALREADY_USED" -> ResponseEntity.badRequest().body(Map.of("message", "This reset link has already been used."));
            default -> ResponseEntity.badRequest().body(Map.of("message", "Invalid reset link. "));
        };
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody
                                            PasswordChangeRequestDTO passwordChangeRequestDTO,
                                            @AuthenticationPrincipal JwtPrincipal userDetails
    ){
        authService.changePassword(userDetails, passwordChangeRequestDTO);
        return ResponseEntity.ok().body(Map.of("message", "Password changed successfully"));
    }

    @GetMapping("/username")
    public String currentUsername(Authentication authentication){
        if(authentication != null) return authentication.getName();
        else return "";
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserDetails(Authentication authentication){
        return ResponseEntity.ok(authService.getCurrentUserDetails(authentication));
    }

//    @PostMapping("/signout")
//    public ResponseEntity<?> signout(){
//        ResponseCookie cleanCookie = authService.logout();
//        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cleanCookie.toString()).body(new MessageResponse("You have been signed out Successfully!!"));
//    }


    @GetMapping("/sellers")
    public ResponseEntity<?> getSellers(
            Authentication authentication,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber
    ){
        Sort sortByAndorder = Sort.by(AppConstants.SORT_USERS_BY).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, Integer.parseInt(AppConstants.PAGE_SIZE), sortByAndorder);
        return ResponseEntity.ok(authService.getAllSellers(pageDetails));
    }

}
