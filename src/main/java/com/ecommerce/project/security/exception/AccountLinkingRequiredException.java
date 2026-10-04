package com.ecommerce.project.security.exception;

/*
* Thrown during OAuth2 login when the returned email matches an existing account,
* but the provider did not confirm the email is verified.
* We refuse to auto-link in this case for security reasons.
* */

public class AccountLinkingRequiredException extends RuntimeException {
    public AccountLinkingRequiredException(String message) {
        super(message);
    }
}
