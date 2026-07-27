package com.ecommerce.project.security.dto;

/*
*  Normalized shape across providers so the rest of app never has to know that
*  Google calls the id field "sub" while GitHub calls it "id", etc.
* */

public record OAuthUserInfo (
    String provider,                // google | github
    String providerUserId,          // Google: sub, GitHub: id
    String email,                   // may be null for GitHub private emails util fetched seperately
    boolean emailVerified,          // Google: email_verified, GitHub: derived from  /user/emails
    String name
) {}
