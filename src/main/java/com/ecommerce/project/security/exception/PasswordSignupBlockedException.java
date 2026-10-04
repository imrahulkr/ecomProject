package com.ecommerce.project.security.exception;


/* *
* Thrown when someone tries to sign up with a password using email that already belongs to an
* OAuth-only account. We never silently attach a password to an existing account from an
* unauthenticated signup form.
* */

public class PasswordSignupBlockedException extends RuntimeException {
    private final String existingProvider;
    public PasswordSignupBlockedException(String message, String existingProvider) {
        super(message);
        this.existingProvider = existingProvider;
    }

    public String getExistingProvider() {
        return existingProvider;
    }
}
