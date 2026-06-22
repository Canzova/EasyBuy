package easybuy.user_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class LoginRequest {
    @NotNull(message = "User name cannot be null or blank")
    private String username;
    @NotNull(message = "Password cannot be null or blank")
    private String password;
}
