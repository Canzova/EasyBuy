package easybuy.user_service.service;

import com.easybuy.common.exceptions.customException.BusinessException;
import com.easybuy.common.exceptions.customException.ResourceNotFoundException;
import easybuy.user_service.configuration.OtpProperties;
import easybuy.user_service.dto.ForgotPasswordRequest;
import easybuy.user_service.dto.ResetPasswordRequest;
import easybuy.user_service.dto.VerifyOtpRequest;
import easybuy.user_service.dto.VerifyOtpResponse;
import easybuy.user_service.entity.PasswordResetToken;
import easybuy.user_service.entity.User;
import easybuy.user_service.repository.PasswordResetTokenRepository;
import easybuy.user_service.repository.RefreshTokenRepository;
import easybuy.user_service.repository.UserRepository;
import easybuy.user_service.service.email.EmailService;
import easybuy.user_service.service.implementations.UserServiceImplementation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private OtpProperties otpProperties;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserServiceImplementation userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setUserId(UUID.randomUUID());
        sampleUser.setUsername("testuser@easybuy.com");
        sampleUser.setPassword("encodedOldPassword");
    }

    @Test
    @DisplayName("processForgotPassword: User exists -> generates OTP and calls emailService")
    void testProcessForgotPassword_Success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("testuser@easybuy.com");

        when(userRepository.findByUsername(request.getEmail())).thenReturn(Optional.of(sampleUser));
        when(otpProperties.getExpirationMinutes()).thenReturn(5);

        userService.processForgotPassword(request);

        verify(passwordResetTokenRepository).invalidateExistingTokens(request.getEmail());

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertEquals("testuser@easybuy.com", savedToken.getEmail());
        assertNotNull(savedToken.getOtp());
        assertEquals(6, savedToken.getOtp().length());
        assertFalse(savedToken.isUsed());

        verify(emailService).sendOtpEmail(eq("testuser@easybuy.com"), eq(savedToken.getOtp()));
    }

    @Test
    @DisplayName("processForgotPassword: User not found -> throws ResourceNotFoundException")
    void testProcessForgotPassword_UserNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@easybuy.com");

        when(userRepository.findByUsername(request.getEmail())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.processForgotPassword(request));
        verify(emailService, never()).sendOtpEmail(any(), any());
    }

    @Test
    @DisplayName("verifyOtp: Valid OTP -> returns resetToken")
    void testVerifyOtp_Success() {
        VerifyOtpRequest request = new VerifyOtpRequest("testuser@easybuy.com", "123456");

        PasswordResetToken token = PasswordResetToken.builder()
                .email("testuser@easybuy.com")
                .otp("123456")
                .otpExpiryTime(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();

        when(passwordResetTokenRepository.findFirstByEmailAndIsUsedFalseOrderByCreatedAtDesc(request.getEmail()))
                .thenReturn(Optional.of(token));
        when(otpProperties.getResetTokenExpirationMinutes()).thenReturn(15);

        VerifyOtpResponse response = userService.verifyOtp(request);

        assertNotNull(response);
        assertNotNull(response.getResetToken());
        assertEquals("OTP verified successfully. You may now reset your password.", response.getMessage());
        assertNotNull(token.getResetToken());
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    @DisplayName("verifyOtp: Expired OTP -> throws BusinessException")
    void testVerifyOtp_Expired() {
        VerifyOtpRequest request = new VerifyOtpRequest("testuser@easybuy.com", "123456");

        PasswordResetToken token = PasswordResetToken.builder()
                .email("testuser@easybuy.com")
                .otp("123456")
                .otpExpiryTime(LocalDateTime.now().minusMinutes(1))
                .isUsed(false)
                .build();

        when(passwordResetTokenRepository.findFirstByEmailAndIsUsedFalseOrderByCreatedAtDesc(request.getEmail()))
                .thenReturn(Optional.of(token));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.verifyOtp(request));
        assertTrue(exception.getMessage().contains("OTP has expired"));
    }

    @Test
    @DisplayName("verifyOtp: Wrong OTP -> throws BusinessException")
    void testVerifyOtp_InvalidOtp() {
        VerifyOtpRequest request = new VerifyOtpRequest("testuser@easybuy.com", "999999");

        PasswordResetToken token = PasswordResetToken.builder()
                .email("testuser@easybuy.com")
                .otp("123456")
                .otpExpiryTime(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();

        when(passwordResetTokenRepository.findFirstByEmailAndIsUsedFalseOrderByCreatedAtDesc(request.getEmail()))
                .thenReturn(Optional.of(token));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.verifyOtp(request));
        assertTrue(exception.getMessage().contains("Invalid OTP"));
    }

    @Test
    @DisplayName("resetPassword: Valid reset token -> updates user password and marks token used")
    void testResetPassword_Success() {
        String resetToken = UUID.randomUUID().toString();
        ResetPasswordRequest request = new ResetPasswordRequest(resetToken, "NewSecurePassword123!");

        PasswordResetToken token = PasswordResetToken.builder()
                .email("testuser@easybuy.com")
                .resetToken(resetToken)
                .resetTokenExpiryTime(LocalDateTime.now().plusMinutes(10))
                .isUsed(false)
                .build();

        when(passwordResetTokenRepository.findByResetTokenAndIsUsedFalse(resetToken)).thenReturn(Optional.of(token));
        when(userRepository.findByUsername("testuser@easybuy.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode("NewSecurePassword123!")).thenReturn("encodedNewPassword");

        userService.resetPassword(request);

        assertEquals("encodedNewPassword", sampleUser.getPassword());
        assertTrue(token.isUsed());
        verify(userRepository).save(sampleUser);
        verify(passwordResetTokenRepository).save(token);
        verify(refreshTokenRepository).deleteByUser(sampleUser);
    }

    @Test
    @DisplayName("resetPassword: Expired reset token -> throws BusinessException")
    void testResetPassword_ExpiredToken() {
        String resetToken = UUID.randomUUID().toString();
        ResetPasswordRequest request = new ResetPasswordRequest(resetToken, "NewSecurePassword123!");

        PasswordResetToken token = PasswordResetToken.builder()
                .email("testuser@easybuy.com")
                .resetToken(resetToken)
                .resetTokenExpiryTime(LocalDateTime.now().minusMinutes(1))
                .isUsed(false)
                .build();

        when(passwordResetTokenRepository.findByResetTokenAndIsUsedFalse(resetToken)).thenReturn(Optional.of(token));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.resetPassword(request));
        assertTrue(exception.getMessage().contains("Reset token has expired"));
        verify(userRepository, never()).save(any());
    }
}
