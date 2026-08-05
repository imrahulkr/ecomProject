package com.ecommerce.project.security.services;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.auth.UserRepository;
import com.ecommerce.project.security.dto.AuthResponse;
import com.ecommerce.project.security.dto.LoginRequest;
import com.ecommerce.project.security.dto.SignupRequest;
import com.ecommerce.project.security.exception.EmailAlreadyRegisteredException;
import com.ecommerce.project.security.exception.PasswordSignupBlockedException;
import com.ecommerce.project.security.jwt.JwtUtils;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SecureAuthServiceImpl implements SecureAuthService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public SecureAuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils, RefreshTokenService refreshTokenService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
    }

    /*
    * Password-based signup. Blocks (does not merge) if the email is already
    * taken -- weather by another password acccount or an OAuth-only account.
    * */

    @Override
    @Transactional
    public IssuedTokens signup(SignupRequest request){
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

        User user = User.builder()
                .email(request.email())
                .name(request.name())
                .password(passwordEncoder.encode(request.password()))
                .enabled(false)
                .build();
        user = userRepository.save(user);
        return issueTokenFor(user);
    }

    @Override
    @Transactional
    public IssuedTokens login(LoginRequest request){
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if(!user.hasPassword()){
            throw new BadCredentialsException("This account uses social login. Please log in with the provider you originally used");
        }
//        if(!passwordEncoder.matches(request.password(), user.getPassword())){
//            throw new BadCredentialsException("Invalid email or password.");
//        }

        Authentication authResult = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
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
        return new AuthResponse(
          issuedTokens.accessToken(),
                jwtUtils.getAccessTokenTtlSeconds(),
                new AuthResponse.UserSummary(
                        user.getUserId().toString(),
                        user.getEmail(),
                        user.getName(),
                        user.hasPassword(),
                        user.getOAuthAccounts().stream().map(a -> a.getProvider()).toList()
                )

        );
    }

    public record IssuedTokens(String accessToken, String refreshToken, User user) {}
}
