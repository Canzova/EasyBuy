package easybuy.user_service.controller;

import easybuy.user_service.configuration.AppConstants;
import easybuy.user_service.dto.UserDTO;
import easybuy.user_service.dto.UserPageResponse;
import easybuy.user_service.dto.UserUpdateRequestDTO;
import easybuy.user_service.dto.UserUpdateResponseDTO;
import easybuy.user_service.entity.User;
import easybuy.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.UUID;

@Controller
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/")
    public ResponseEntity<UserDTO>registerUser(@Valid @RequestBody UserDTO userDTO){
        UserDTO registeredUser = userService.registerUser(userDTO);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO> getUserByUserId(@PathVariable UUID userId){
        UserDTO userDTO = userService.getUserByUserId(userId);
        return ResponseEntity.ok(userDTO);
    }

    @GetMapping("email/{email}")
    public ResponseEntity<UserDTO> getUserByGivenEmail(@PathVariable String email){
        UserDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/")
    public ResponseEntity<UserPageResponse> getAllUsers(@RequestParam(name = "pageNo", defaultValue = AppConstants.PAGE_NO, required = false) int pageNo,
                                                        @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) int pageSize,
                                                        @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_BY, required = false) String sortBy,
                                                        @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER) String sortOrder){
        UserPageResponse userPageResponse = userService.getAllUsers(pageNo, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(userPageResponse);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserUpdateResponseDTO> updateUser(@Valid @RequestBody UserUpdateRequestDTO userDTO, @PathVariable UUID userId){
        UserUpdateResponseDTO updatedUserDTO = userService.updateUser(userDTO, userId);
        return ResponseEntity.ok(updatedUserDTO);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId){
        userService.deleteUserByUserId(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
