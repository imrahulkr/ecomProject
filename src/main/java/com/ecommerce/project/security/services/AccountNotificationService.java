package com.ecommerce.project.security.services;


/*
* Sends Security-relevant account notifications. These matter because they're the user's
* only signal if an account gets auto-linked to a new provider (or unlinked) and they weren't
* the one who did it.
*
* uses Spring mail (JavaMailSender) - configure spring.mail.* properties per environment.
* Swap SimpleMailMessage for a proper HTML template (Thymeleaf/Freemarker) once you need
* branded emails;
* */
public class AccountNotificationService {

}
