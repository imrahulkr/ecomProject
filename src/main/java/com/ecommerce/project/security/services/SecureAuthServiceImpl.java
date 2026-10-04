package com.ecommerce.project.security.services;

import com.ecommerce.project.auth.AppRole;
import com.ecommerce.project.auth.Role;
import com.ecommerce.project.auth.RoleRepository;
import com.ecommerce.project.auth.User;
import com.ecommerce.project.auth.UserRepository;
import com.ecommerce.project.notification.email.event.OnUserRegisteredEvent;
import com.ecommerce.project.security.OAuthAccountRepository;
import com.ecommerce.project.security.dto.AuthResponse;
import com.ecommerce.project.security.dto.LoginRequest;
import com.ecommerce.project.security.dto.MessageResponse;
import com.ecommerce.project.security.dto.SignupRequest;
import com.ecommerce.project.security.exception.EmailAlreadyRegisteredException;
import com.ecommerce.project.security.exception.PasswordSignupBlockedException;
import com.ecommerce.project.security.jwt.JwtUtils;
import jakarta.transaction.Transactional;

import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class SecureAuthServiceImpl implements SecureAuthService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final OAuthAccountRepository oAuthAccountRepository;

    public SecureAuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils, RefreshTokenService refreshTokenService, AuthenticationManager authenticationManager, RoleRepository roleRepository, ApplicationEventPublisher eventPublisher, OAuthAccountRepository oAuthAccountRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
        this.roleRepository = roleRepository;
        this.eventPublisher = eventPublisher;
        this.oAuthAccountRepository = oAuthAccountRepository;
    }

    /*
    * Password-based signup. Blocks (does not merge) if the email is already
    * taken -- weather by another password acccount or an OAuth-only account.
    * */

    @Override
    @Transactional
    public ResponseEntity<MessageResponse> signup(SignupRequest request){
        Optional<User> existing = userRepository.findByEmail(request.email());

        if(existing.isPresent()){
            User user = existing.get();
            if(user.hasPassword()){
                throw new EmailAlreadyRegisteredException("An account with that email already exists." +
                        "Please login instead.");
            } else {
                String linkedProvider = user.getOAuthAccounts().isEmpty()
                        ?"a social login"
                        : user.getOAuthAccounts().get(0).getProvider();
                throw new PasswordSignupBlockedException(
                        "This email is linked to a " + linkedProvider + " account. Please log in with " +
                                linkedProvider + ", then set a password from account settings.", linkedProvider
                );
            }
        }

        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error : Role is not Found !!!!!! "));

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .name(request.name())
                .password(passwordEncoder.encode(request.password()))
                .enabled(false)
                .roles(Set.of(userRole))
                .build();
        user = userRepository.save(user);
        eventPublisher.publishEvent(new OnUserRegisteredEvent(this, user));
         return ResponseEntity.ok(new MessageResponse("User registered successfully, Kindly verify your email!"));
        // return issueTokenFor(user);
    }

    @Override
    @Transactional
    public IssuedTokens login(LoginRequest request){
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if(!user.hasPassword()){
            throw new BadCredentialsException("This account uses social login. Please log in with the provider you originally used");
        }

        // Unverified accounts: only someone who knows the password learns the account exists and
        // unverified, or can trigger a new verification email. (Checked by hand because
        // DaoAuthenticationProvider rejects a disabled user before it ever checks the password.)
        if(!user.isEnabled()){
            if(!passwordEncoder.matches(request.password(), user.getPassword())){
                throw new BadCredentialsException("Invalid email or password.");
            }
            eventPublisher.publishEvent(new OnUserRegisteredEvent(this, user));
            throw new BadCredentialsException("Account is not verified. We've sent a new verification email.");
        }

        // Authenticate the account resolved from the email, never a client-supplied username -
        // otherwise a valid username/password for one account would issue tokens for another email.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.password())
        );

        return issueTokenFor(user);
    }

    public IssuedTokens issueTokenFor(User user){
        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshToken = refreshTokenService.issueNewChain(user);
        return new IssuedTokens(accessToken, refreshToken, user);
    }

    @Override
    public AuthResponse toAuthResponse(IssuedTokens issuedTokens){
        User user = issuedTokens.user();
        // Query directly rather than navigating user.getOAuthAccounts() (a lazy @OneToMany) - this
        // method's callers may hand it a User fetched in an already-closed transaction (e.g.
        // RefreshTokenService.rotate), so it can't rely on the caller having pre-touched the
        // collection. Same pattern as AccountLinkController.
        List<String> providers = oAuthAccountRepository.findByUser(user).stream()
                .map(a -> a.getProvider())
                .toList();
        return new AuthResponse(
          issuedTokens.accessToken(),
                jwtUtils.getAccessTokenTtlSeconds(),
                new AuthResponse.UserSummary(
                        user.getUserId().toString(),
                        user.getEmail(),
                        user.getName(),
                        user.hasPassword(),
                        providers
                )

        );
    }

    public record IssuedTokens(String accessToken, String refreshToken, User user) {}
}
