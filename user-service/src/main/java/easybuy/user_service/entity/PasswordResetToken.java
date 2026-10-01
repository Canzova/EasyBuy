package easybuy.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_token", indexes = {
        @Index(name = "idx_password_reset_email", columnList = "email"),
        @Index(name = "idx_password_reset_token", columnList = "resetToken")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 6)
    private String otp;

    @Column(nullable = false)
    private LocalDateTime otpExpiryTime;

    @Column(unique = true)
    private String resetToken;

    private LocalDateTime resetTokenExpiryTime;

    @Column(nullable = false)
    private boolean isUsed;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
