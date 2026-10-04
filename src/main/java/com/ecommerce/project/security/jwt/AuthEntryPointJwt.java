package com.ecommerce.project.security.jwt;

import com.ecommerce.project.payload.APIResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

// Fires before @RestControllerAdvice ever runs - Spring Security's filter chain rejects
// unauthenticated requests up front, so this builds the same APIResponse envelope
// MyGlobalExceptionHandler uses rather than going through it.
//
// Deliberately a plain `new ObjectMapper()` rather than an injected bean: Boot's own
// autoconfigured ObjectMapper is tools.jackson.databind.ObjectMapper (Spring Boot 4's Jackson 3),
// not com.fasterxml.jackson.databind.ObjectMapper - injecting the latter has no matching bean
// and fails context startup. APIResponse.timestamp is a pre-formatted String for the same
// reason (see its javadoc), so no java.time module registration is needed here either.
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        logger.warn("Unauthorized request to {}: {}", request.getRequestURI(), authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        APIResponse body = APIResponse.builder()
                .timestamp(Instant.now().toString())
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .error("UNAUTHORIZED")
                .message("Missing or invalid access token")
                .path(request.getRequestURI())
                .build();

        objectMapper.writeValue(response.getWriter(), body);
    }
}
