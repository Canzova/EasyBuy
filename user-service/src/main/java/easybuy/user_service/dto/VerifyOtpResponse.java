package easybuy.user_service.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VerifyOtpResponse {

    private String message;
    private String resetToken;
}
