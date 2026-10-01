package easybuy.user_service.repository;

import easybuy.user_service.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findFirstByEmailAndIsUsedFalseOrderByCreatedAtDesc(String email);

    Optional<PasswordResetToken> findByResetTokenAndIsUsedFalse(String resetToken);

    // Modify is needed if we are writing any query apart from select. here is update
    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.isUsed = true WHERE p.email = :email AND p.isUsed = false")
    void invalidateExistingTokens(@Param("email") String email);
}
