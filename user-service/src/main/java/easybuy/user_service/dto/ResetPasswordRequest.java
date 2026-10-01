package easybuy.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token cannot be blank or null.")
    private String resetToken;

    @NotBlank(message = "New password cannot be blank or null.")
    @Size(min = 5, message = "Password should have at-least 5 characters.")
    private String newPassword;
}
