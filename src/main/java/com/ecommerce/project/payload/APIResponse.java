package com.ecommerce.project.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

// Canonical error envelope for every non-2xx JSON response (validation errors, business
// exceptions, 401s, 403s) - see MyGlobalExceptionHandler, AuthEntryPointJwt and
// RestAccessDeniedHandler, the three places that produce error bodies. errors is only
// populated for field-level validation failures; omitted (not null) everywhere else.
// timestamp is a pre-formatted ISO-8601 string (Instant.now().toString()), not a raw Instant -
// AuthEntryPointJwt/RestAccessDeniedHandler serialize this with a plain `new ObjectMapper()`
// (no java.time module registered), and this app has two incompatible Jackson major versions on
// the classpath (Spring Boot 4's own tools.jackson.databind, plus com.fasterxml.jackson.databind
// pulled in transitively by jjwt/AWS SDK) so a raw Instant field is one module-registration
// mismatch away from an InvalidDefinitionException in whichever serializer path skips it.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIResponse {
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> errors;
}
