package easybuy.user_service.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class LoginResponse {

    private String username;
    private String accessToken;
    private String refreshToken;

}
