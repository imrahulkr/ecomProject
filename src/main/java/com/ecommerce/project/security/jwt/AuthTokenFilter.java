package com.ecommerce.project.security.jwt;

import com.ecommerce.project.security.OAuthAccount;
import com.ecommerce.project.security.dto.JwtPrincipal;
import com.ecommerce.project.security.services.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

//JWTAuthenticationFilter

@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        logger.debug("AuthTokenFilter called for URI: {}", request.getRequestURI());

        String header = request.getHeader("Authorization");
        if(header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtils.parseAndValidate(token);
                String username = claims.getSubject();
                String email = claims.get("email", String.class);
                Long userId = claims.get("userId", Long.class);
                boolean enabled = claims.get("enabled", Boolean.class);
                List<String> roles = claims.get("roles", List.class);
                Collection<? extends GrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                List<OAuthAccount> oAuthAccounts = claims.get("oAuthAccounts", List.class);
                JwtPrincipal principal = new JwtPrincipal(username, email, userId, authorities, oAuthAccounts, enabled);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal,
                                null,
                                userDetails.getAuthorities());
                logger.debug("Roles from JWT: {}", userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                logger.error("Cannot set user authentication: {}", e);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String jwtFromCookies = jwtUtils.getJwtFromCookies(request);
        if(jwtFromCookies != null) {
            logger.debug("AuthTokenFilter.java jwtFromCookies: {}", jwtFromCookies);
            return jwtFromCookies;
        }
        String jwtFromHeader = jwtUtils.getJwtFromHeader(request);
        logger.debug("AuthTokenFilter.java: jwtFromHeader {}", jwtFromHeader);
        return jwtFromHeader;
    }

}
