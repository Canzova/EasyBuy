package easybuy.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import easybuy.user_service.dto.ForgotPasswordRequest;
import easybuy.user_service.dto.ResetPasswordRequest;
import easybuy.user_service.dto.VerifyOtpRequest;
import easybuy.user_service.dto.VerifyOtpResponse;
import easybuy.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    @DisplayName("POST /api/users/forgot-password: Valid email -> 200 OK")
    void testForgotPassword_Success() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@example.com");

        doNothing().when(userService).processForgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/users/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent successfully to your registered email."));

        verify(userService).processForgotPassword(any(ForgotPasswordRequest.class));
    }

    @Test
    @DisplayName("POST /api/users/verify-otp: Valid OTP -> 200 OK with resetToken")
    void testVerifyOtp_Success() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("user@example.com", "123456");
        VerifyOtpResponse response = VerifyOtpResponse.builder()
                .message("OTP verified successfully.")
                .resetToken("test-reset-token-uuid")
                .build();

        when(userService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").value("test-reset-token-uuid"))
                .andExpect(jsonPath("$.message").value("OTP verified successfully."));

        verify(userService).verifyOtp(any(VerifyOtpRequest.class));
    }

    @Test
    @DisplayName("POST /api/users/reset-password: Valid request -> 200 OK")
    void testResetPassword_Success() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("test-reset-token-uuid", "NewPassword123!");

        doNothing().when(userService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/users/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been reset successfully. You can now login with your new password."));

        verify(userService).resetPassword(any(ResetPasswordRequest.class));
    }
}
