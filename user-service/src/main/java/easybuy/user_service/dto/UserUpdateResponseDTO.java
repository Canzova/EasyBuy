package easybuy.user_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateResponseDTO {
    private String name;
    private String address;
    private String phone;
}
