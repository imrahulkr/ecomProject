package com.ecommerce.project.controller;

import com.ecommerce.project.config.AppConstants;

import com.ecommerce.project.payload.AuthenticationResult;
import com.ecommerce.project.payload.ForgotPasswordRequestDTO;
import com.ecommerce.project.payload.ResetPasswordRequestDTO;

import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.MessageResponse;

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

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
//    @Autowired
//    private JwtUtils jwtUtils;
//    @Autowired
//    private AuthenticationManager authenticationManager;
//
//    @Autowired
//    UserRepository userRepository;
//
//    @Autowired
//    PasswordEncoder encoder;
//
//    @Autowired
//    RoleRepository roleRepository;

    @Autowired
    AuthService authService;
    @Autowired
    RateLimiterService rateLimiterService;
    @Autowired
    PasswordResetTokenService passwordResetTokenService;

@PostMapping("/signin")
public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
    AuthenticationResult authenticationResult = authService.login(loginRequest);
//    AuthenticationResult authenticationResult;
//    try{
//        authenticationResult = authService.login(loginRequest);
//    } catch (AuthenticationException exception) {
//        Map<String, Object> map = new HashMap<>();
//        map.put("message", "Bad credentials");
//        map.put("status", false);
//        return new ResponseEntity<>(map, HttpStatus.NOT_FOUND);
//    }
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, authenticationResult.getJwtCookie().toString()).body(authenticationResult.getResponse());
}

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest){
        return authService.register(signupRequest);
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyAccount(@RequestParam("token") String token){
        return authService.validateVerificationToken(token);
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
    public ResponseEntity<?> validateResetToken(@RequestParam("token") String token){
        String result = passwordResetTokenService.validateToken(token);
        return switch (result) {
            case "VALID" -> ResponseEntity.ok().body(Map.of("valid", true));
            case "EXPIRED" -> ResponseEntity.badRequest().body(Map.of("valid", false, "reason", "This reset link has expired. Kindly request a new one."));
            case "ALREADY_USED" -> ResponseEntity.badRequest().body(Map.of("valid", false, "reason", "This reset link has already been used."));
            default -> ResponseEntity.badRequest().body(Map.of("valid", false, "reason", "Invalid reset link. "));
        };
    }


    @GetMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody
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

    @GetMapping("/username")
    public String currentUsername(Authentication authentication){
        if(authentication != null) return authentication.getName();
        else return "";
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserDetails(Authentication authentication){

        return ResponseEntity.ok(authService.getCurrentUserDetails(authentication));
    }

    @PostMapping("/signout")
    public ResponseEntity<?> signout(){
//        ResponseCookie cleanCookie = jwtUtils.getJwtCleanCookies();
        ResponseCookie cleanCookie = authService.logout();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cleanCookie.toString()).body(new MessageResponse("You have been signed out Successfully!!"));
    }


    @GetMapping("/sellers")
    public ResponseEntity<?> getSellers(
            Authentication authentication,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber
    ){
        Sort sortByAndorder = Sort.by(AppConstants.SORT_USERS_BY).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, Integer.parseInt(AppConstants.PAGE_SIZE), sortByAndorder);
        return ResponseEntity.ok(authService.getAllSellers(pageDetails));
    }


    //    @PostMapping("/signin")
//    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
//        Authentication authentication;
//        try{
//            authentication = authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
//            );
//        } catch (AuthenticationException exception) {
//            Map<String, Object> map = new HashMap<>();
//            map.put("message", "Bad credentials");
//            map.put("status", false);
//            return new ResponseEntity<>(map, HttpStatus.NOT_FOUND);
//        }
//
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
//        //String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);
//        ResponseCookie jwtCookies = jwtUtils.generateJwtCookies(userDetails);
//
//        List<String> roles = userDetails.getAuthorities().stream()
//                .map(item -> item.getAuthority())
//                .collect(Collectors.toList());
////         UserInfoResponse response = new UserInfoResponse(userDetails.getId(), userDetails.getEmail(), userDetails.getUsername(), roles, jwtToken);
//        UserInfoResponse response = new UserInfoResponse(userDetails.getId(), userDetails.getEmail(), userDetails.getUsername(), roles, jwtCookies.toString());
//        // return ResponseEntity.ok(response);
////        UserInfoResponse response = new UserInfoResponse(userDetails.getId(), userDetails.getUsername(), roles);
//        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookies.toString()).body(response);
//    }

    //    @PostMapping("/signup")
//    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest){
//        if(userRepository.existsByUsername(signupRequest.getUsername())) {
//            //return new ResponseEntity<>("Username is already in use", HttpStatus.BAD_REQUEST);
//            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username already exist!!"));
//        }
//        if(userRepository.existsByEmail(signupRequest.getEmail())){
//            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email already exist!!"));
//        }
//        User user = new User(
//                signupRequest.getUsername(),
//                signupRequest.getEmail(),
//                encoder.encode(signupRequest.getPassword())
//        );
//
//        Set<String> strRoles = signupRequest.getRole();
//        Set<Role> roles = new HashSet<>();
//
//        if(strRoles==null) {
//            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
//                    .orElseThrow(() -> new RuntimeException("Error : Role is not Found !!!!!! "));
//            roles.add(userRole);
//        } else{
//            strRoles.forEach(role -> {
//                switch (role) {
//                    case "admin":
//                        Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
//                                .orElseThrow(() -> new RuntimeException("Error : Role is not Found !!!!!! "));
//                        roles.add(adminRole);
//                        break;
//                    case "seller":
//                        Role sellerRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER)
//                                .orElseThrow(() -> new RuntimeException("Error : Role is not Found !!!!!! "));
//                        roles.add(sellerRole);
//                        break;
//                    default:
//                        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
//                                .orElseThrow(() -> new RuntimeException("Error : Role is not Found !!!!!! "));
//                        roles.add(userRole);
//                }
//            });
//        }
//        user.setRoles(roles);
//        userRepository.save(user);
//        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
//    }


    //    @GetMapping("/user")
//    public ResponseEntity<?> getUserDetails(Authentication authentication){
//        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
//        List<String> roles = userDetails.getAuthorities().stream()
//                .map(item -> item.getAuthority())
//                .collect(Collectors.toList());
//        UserInfoResponse response = new UserInfoResponse(userDetails.getId(), userDetails.getUsername(), roles);
//        return ResponseEntity.ok(response);
//    }

}
