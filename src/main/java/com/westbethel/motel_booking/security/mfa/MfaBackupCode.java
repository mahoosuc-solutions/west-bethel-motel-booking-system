package com.westbethel.motel_booking.security.mfa;

import com.westbethel.motel_booking.security.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing an MFA backup code.
 * Backup codes are single-use and hashed for security.
 *
 * @author Security Agent 1 - Phase 2
 */
@Entity
@Table(name = "mfa_backup_codes", indexes = {
        @Index(name = "idx_mfa_backup_codes_user_id", columnList = "user_id"),
        @Index(name = "idx_mfa_backup_codes_used", columnList = "used")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaBackupCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "used", nullable = false)
    @Builder.Default
    private Boolean used = false;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Mark backup code as used.
     */
    public void markAsUsed() {
        this.used = true;
        this.usedAt = OffsetDateTime.now();
    }
}
