package easybuy.user_service.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RefreshTokenResponse {
    private String username;
    private String accessToken;
    private String refreshToken;
}
