package com.ecommerce.project.security.jwt;

import com.ecommerce.project.payload.APIResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

// Fires for an authenticated user who fails a role check (e.g. ROLE_USER hitting /api/admin/**).
// Without this, Spring Security falls through to its default whitelabel/error response instead
// of the APIResponse envelope every other error path uses - see AuthEntryPointJwt (401 sibling)
// and MyGlobalExceptionHandler (everything past the security filter chain).
//
// Deliberately a plain `new ObjectMapper()` rather than an injected bean - see AuthEntryPointJwt's
// javadoc for why (Boot's autoconfigured ObjectMapper bean is Jackson 3's tools.jackson type here,
// not com.fasterxml.jackson.databind.ObjectMapper).
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        logger.warn("Access denied for {} {}: {}", request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        APIResponse body = APIResponse.builder()
                .timestamp(Instant.now().toString())
                .status(HttpServletResponse.SC_FORBIDDEN)
                .error("FORBIDDEN")
                .message("You do not have permission to perform this action")
                .path(request.getRequestURI())
                .build();

        objectMapper.writeValue(response.getWriter(), body);
    }
}
