package com.ecommerce.project.security.services;

import com.ecommerce.project.security.OAuthAccount;
import com.ecommerce.project.auth.User;
import com.ecommerce.project.security.OAuthAccountRepository;
import com.ecommerce.project.auth.UserRepository;
import com.ecommerce.project.security.dto.OAuthUserInfo;
import com.ecommerce.project.security.exception.AccountLinkingRequiredException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OAuthUserService {

    private final UserRepository userRepository;
//    private final AccountNotificationService notificationService;
    private final OAuthAccountRepository oAuthAccountRepository;

    public OAuthUserService(UserRepository userRepository, OAuthAccountRepository oAuthAccountRepository) {
        this.userRepository = userRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
    }

    /*
    * Core Entry point called form the OAuth2 success handler.
    *
    * Rules:
    *  1. Already linked (provider + providerUserId known) -> just return that user.
    *  2. No Existing user with this email -> create a brand new user + link.
    *  3. Existing user with this email, provider confirms email verified -> auto link + notify.
    *  4. Existing user with this email, email NOT verified by provider -> refuse to auto-link;
    *       caller must log in with the password first and link manually from account settings.
    * */

    @Transactional
    public User findOrCreateFromOAuth(OAuthUserInfo info){

        Optional<OAuthAccount> existingLink = oAuthAccountRepository.findByProviderAndProviderUserId(info.provider(), info.providerUserId());
        if(existingLink.isPresent()) return existingLink.get().getUser();

        if(info.email() == null) throw new IllegalStateException(
                "Provider " + info.provider() + " did not return an email. Cannot proceed with login."
        );

        Optional<User> existingUser = userRepository.findByEmail(info.email());

        if(existingUser.isPresent()){
            User user = existingUser.get();

            if(!info.emailVerified()) throw new AccountLinkingRequiredException(
                    "An account with this email already exists. Please log in with your password " +
                            "and link " + info.provider() + " from account settings. "
            );

            // provider has already verified ownership of this email - safe to auto-link.
            linkAccount(user, info);
//     ======== //notificationService.notifyNewProviderLinked(user, info.provider()); ========
            return user;
        }

        // Brand new user, first time we've seen this email at all.
//        User newUser = new User();
//        newUser.setEmail(info.email());
//        newUser.setName(info.name());
//        newUser.setEnabled(info.emailVerified());
//        newUser = userRepository.save(newUser);

        User newUser = User.builder()
                .email(info.email())
                .name(info.name())
                .username(info.email().split("@")[0])
                .enabled(info.emailVerified())
                .build();
        return newUser;
    }

    private void linkAccount(User user, OAuthUserInfo info){
        OAuthAccount account = OAuthAccount.builder()
                .user(user)
                .provider(info.provider())
                .providerUserId(info.providerUserId())
                .build();
        oAuthAccountRepository.save(account);
    }
}
