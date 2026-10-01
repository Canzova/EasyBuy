package easybuy.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ForgotPasswordRequest {

    @NotBlank(message = "Email cannot be blank or null.")
    @Email(message = "Please provide a valid email address.")
    private String email;
}
