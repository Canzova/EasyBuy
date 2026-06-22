package easybuy.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RefreshTokenRequest {
    @NotBlank(message = "User name cannot be null or blank.")
    private String username;

    @NotBlank(message = "Refresh token cannot be null or blank")
    private String refreshToken;
}
