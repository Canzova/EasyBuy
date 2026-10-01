package easybuy.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VerifyOtpRequest {

    @NotBlank(message = "Email cannot be blank or null.")
    @Email(message = "Please provide a valid email address.")
    private String email;

    @NotBlank(message = "OTP cannot be blank or null.")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits.")
    private String otp;
}
