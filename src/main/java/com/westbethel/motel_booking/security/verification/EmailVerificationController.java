package com.westbethel.motel_booking.security.verification;

import com.westbethel.motel_booking.security.verification.dto.VerificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for email verification operations.
 *
 * @author Security Agent 1 - Phase 2
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationController {

    private final EmailVerificationService verificationService;

    /**
     * Verify email using a token.
     *
     * @param token the verification token
     * @return success response
     */
    @PostMapping("/verify-email")
    public ResponseEntity<VerificationResponse> verifyEmail(
            @RequestParam String token
    ) {
        try {
            verificationService.verifyEmail(token);

            return ResponseEntity.ok(
                    VerificationResponse.builder()
                            .success(true)
                            .message("Email verified successfully")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(VerificationResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error verifying email", e);
            return ResponseEntity.internalServerError()
                    .body(VerificationResponse.builder()
                            .success(false)
                            .message("An error occurred while verifying email")
                            .build());
        }
    }

    /**
     * Resend verification email to authenticated user.
     *
     * @param authentication the authenticated user
     * @return success response
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<VerificationResponse> resendVerification(
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            verificationService.resendVerification(username);

            return ResponseEntity.ok(
                    VerificationResponse.builder()
                            .success(true)
                            .message("Verification email sent")
                            .build()
            );
        } catch (IllegalStateException e) {
            // Rate limit or already verified
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(VerificationResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(VerificationResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error resending verification email", e);
            return ResponseEntity.internalServerError()
                    .body(VerificationResponse.builder()
                            .success(false)
                            .message("An error occurred while sending verification email")
                            .build());
        }
    }
}
