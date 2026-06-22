package easybuy.user_service.service;

import easybuy.user_service.dto.*;
import easybuy.user_service.entity.User;
import jakarta.validation.Valid;

import java.util.UUID;

public interface UserService {
    UserDTO registerUser(easybuy.user_service.dto.UserDTO userDTO);

    UserDTO getUserByUserId(UUID userId);

    UserDTO getUserByEmail(String email);

    UserPageResponse getAllUsers(int pageNo, int pageSize, String sortBy, String sortOrder);

    UserUpdateResponseDTO updateUser(UserUpdateRequestDTO userDTO, UUID userId);

    void deleteUserByUserId(UUID userId);

    LoginResponse loginUser(@Valid LoginRequest loginRequest);

    RefreshTokenResponse updateRefreshAndAccessToken(@Valid RefreshTokenRequest refreshTokenRequest);
}
