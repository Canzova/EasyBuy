package easybuy.user_service.repository;

import easybuy.user_service.entity.RefreshToken;
import easybuy.user_service.entity.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUser_Username(@NotBlank(message = "User name cannot be null or blank.") String username);

    Optional<RefreshToken> findByUser(User user);
}
