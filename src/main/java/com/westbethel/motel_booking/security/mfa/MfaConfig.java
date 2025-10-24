package com.westbethel.motel_booking.security.mfa;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for Multi-Factor Authentication.
 *
 * @author Security Agent 1 - Phase 2
 */
@Configuration
public class MfaConfig {

    /**
     * Configure Google Authenticator for TOTP.
     * Uses standard TOTP parameters: 30 second time step, 6 digit codes.
     *
     * @return configured GoogleAuthenticator
     */
    @Bean
    public GoogleAuthenticator googleAuthenticator() {
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setTimeStepSizeInMillis(TimeUnit.SECONDS.toMillis(30))
                .setWindowSize(3) // Allow 1.5 minutes window (3 * 30 seconds)
                .setCodeDigits(6)
                .setKeyRepresentation(GoogleAuthenticatorConfig.KeyRepresentation.BASE32)
                .build();

        return new GoogleAuthenticator(config);
    }
}
