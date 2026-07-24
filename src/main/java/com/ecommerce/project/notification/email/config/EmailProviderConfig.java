package com.ecommerce.project.notification.email.config;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailProviderConfig {
    @Value("${resend.api-key}")
    private String resendApiKey;

    @Bean
    @ConditionalOnProperty(name="app.email.provider", havingValue="resend")
    public Resend resendClient(){
        return new Resend(resendApiKey);
    }

}
