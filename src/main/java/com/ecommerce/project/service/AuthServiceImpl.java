package com.ecommerce.project.service;

import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.model.UserVerificationToken;
import com.ecommerce.project.notification.email.event.OnPasswordResetRequestedEvent;
import com.ecommerce.project.notification.email.event.OnRegistrationCompleteEvent;
import com.ecommerce.project.notification.email.event.OnUserRegisteredEvent;
import com.ecommerce.project.payload.AuthenticationResult;
import com.ecommerce.project.payload.ForgotPasswordRequestDTO;
import com.ecommerce.project.payload.UserDTO;
import com.ecommerce.project.payload.UserResponse;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.repositories.UserVerificationTokenRepository;
import com.ecommerce.project.security.jwt.JwtUtils;
import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.MessageResponse;
import com.ecommerce.project.security.response.UserInfoResponse;
import com.ecommerce.project.security.services.UserDetailsImpl;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthServiceImpl implements AuthService{
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    private UserVerificationTokenRepository userVerificationTokenRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public AuthenticationResult login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        ResponseCookie jwtCookies = jwtUtils.generateJwtCookies(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());
        UserInfoResponse response = new UserInfoResponse(userDetails.getId(), userDetails.getEmail(), userDetails.getUsername(), roles, jwtCookies.toString());
        return new AuthenticationResult(response, jwtCookies);
    }

    @Override
    public ResponseEntity<MessageResponse> register(SignupRequest signupRequest) {
        if(userRepository.existsByUsername(signupRequest.getUsername())) {
            //return new ResponseEntity<>("Username is already in use", HttpStatus.BAD_REQUEST);
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username already exist!!"));
        }
        if(userRepository.existsByEmail(signupRequest.getEmail())){
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email already exist!!"));
        }
        User user = new User(
                signupRequest.getUsername(),
                signupRequest.getEmail(),
                encoder.encode(signupRequest.getPassword())
        );

        Set<String> strRoles = signupRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if(strRoles==null) {
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error : Role is not Found !!!!!! "));
            roles.add(userRole);
        } else{
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error : Role is not Found !!!!!! "));
                        roles.add(adminRole);
                        break;
                    case "seller":
                        Role sellerRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER)
                                .orElseThrow(() -> new RuntimeException("Error : Role is not Found !!!!!! "));
                        roles.add(sellerRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error : Role is not Found !!!!!! "));
                        roles.add(userRole);
                }
            });
        }
        user.setRoles(roles);
        User savedUser = userRepository.save(user);
//        eventPublisher.publishEvent(new OnRegistrationCompleteEvent(savedUser));
        eventPublisher.publishEvent(new OnUserRegisteredEvent(savedUser, savedUser));
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @Override
    public ResponseEntity<Map<String, String>> forgotPassword(ForgotPasswordRequestDTO forgotPasswordRequestDTO) {

        Optional<User> userOpt = userRepository.findByEmail(forgotPasswordRequestDTO.getEmail());
        if(userOpt.isPresent()) {
            User user = userOpt.get();
            if(user.isEnabled())
                eventPublisher.publishEvent(new OnPasswordResetRequestedEvent(this, user));
        }

        return ResponseEntity.ok(Map.of("message", "If account exists with this email, a password reset link has been sent."));
    }

    @Override
    public void saveVerificationTokenForUser(User user, String token){
        UserVerificationToken userVerificationToken = new UserVerificationToken(token, user);
        userVerificationTokenRepository.save(userVerificationToken);
    }

    @Override
    public ResponseEntity<MessageResponse> validateVerificationToken(String token){
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
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());
        return new UserInfoResponse(userDetails.getId(), userDetails.getUsername(), roles);
    }

    @Override
    public ResponseCookie logout() {
        return jwtUtils.getJwtCleanCookies();
    }

    @Override
    public UserResponse getAllSellers(Pageable pageDetails) {
        Page<User> allSellerUsers = userRepository.findByRoleName(AppRole.ROLE_SELLER, pageDetails);

        List<UserDTO> userDTOS = allSellerUsers.getContent().stream()
                .map(user -> modelMapper.map(user, UserDTO.class))
                .collect(Collectors.toList());

        UserResponse userResponse = new UserResponse();
        userResponse.setContent(userDTOS);
        userResponse.setPageNumber(allSellerUsers.getNumber());
        userResponse.setPageSize(allSellerUsers.getSize());
        userResponse.setTotalElements(allSellerUsers.getTotalElements());
        userResponse.setTotalPages(allSellerUsers.getTotalPages());
        userResponse.setLastPage(allSellerUsers.isLast());

        return userResponse;
    }
}
