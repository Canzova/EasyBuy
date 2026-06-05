package easybuy.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequestDTO {

    @NotBlank(message = "Name cannot be blank or null.")
    @Size(min = 3, max = 25, message = "Name should have at-least 3 and at-max 25 characters.")
    private String name;

    @NotBlank(message = "Address cannot be blank or null.")
    @Size(min = 5, max = 50, message = "Address should have at-least 5 and at-max 50 characters.")
    private String address;

    @NotBlank(message = "Phone cannot be blank or null.")
    @Size(min = 10, max = 10, message = "Phone number should be of exactly 10 digits")
    private String phone;
}
