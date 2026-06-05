package easybuy.user_service.service;

import easybuy.user_service.dto.UserDTO;
import easybuy.user_service.dto.UserPageResponse;
import easybuy.user_service.dto.UserUpdateRequestDTO;
import easybuy.user_service.dto.UserUpdateResponseDTO;
import easybuy.user_service.entity.User;

import java.util.UUID;

public interface UserService {
    UserDTO registerUser(easybuy.user_service.dto.UserDTO userDTO);

    UserDTO getUserByUserId(UUID userId);

    UserDTO getUserByEmail(String email);

    UserPageResponse getAllUsers(int pageNo, int pageSize, String sortBy, String sortOrder);

    UserUpdateResponseDTO updateUser(UserUpdateRequestDTO userDTO, UUID userId);

    void deleteUserByUserId(UUID userId);
}
