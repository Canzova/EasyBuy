package easybuy.user_service.service.email;

import com.easybuy.common.exceptions.customException.BusinessException;
import easybuy.user_service.configuration.AwsSesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AwsSesEmailServiceTest {

    @Mock
    private SesV2Client sesV2Client;

    @Mock
    private AwsSesProperties awsSesProperties;

    @InjectMocks
    private AwsSesEmailService awsSesEmailService;

    @BeforeEach
    void setUp() {
        lenient().when(awsSesProperties.getFromEmail()).thenReturn("EasyBuy <noreply@easybuy.com>");
    }

    @Test
    @DisplayName("sendOtpEmail: Dispatches request to AWS SES v2 successfully")
    void testSendOtpEmail_Success() {
        SendEmailResponse mockResponse = SendEmailResponse.builder()
                .messageId("ses-msg-id-98765")
                .build();

        when(sesV2Client.sendEmail(any(SendEmailRequest.class))).thenReturn(mockResponse);

        assertDoesNotThrow(() -> awsSesEmailService.sendOtpEmail("user@example.com", "654321"));

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesV2Client).sendEmail(captor.capture());

        SendEmailRequest capturedRequest = captor.getValue();
        assertEquals("EasyBuy <noreply@easybuy.com>", capturedRequest.fromEmailAddress());
        assertTrue(capturedRequest.destination().toAddresses().contains("user@example.com"));
        assertEquals("EasyBuy - Password Reset OTP", capturedRequest.content().simple().subject().data());
        assertTrue(capturedRequest.content().simple().body().html().data().contains("654321"));
    }

    @Test
    @DisplayName("sendOtpEmail: Handles SesV2Exception and translates to BusinessException")
    void testSendOtpEmail_SesV2Exception() {
        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("MessageRejected")
                .errorMessage("Email address is not verified in SES sandbox")
                .build();

        SesV2Exception sesException = (SesV2Exception) SesV2Exception.builder()
                .statusCode(400)
                .awsErrorDetails(errorDetails)
                .message("Email address is not verified in SES sandbox")
                .build();

        when(sesV2Client.sendEmail(any(SendEmailRequest.class))).thenThrow(sesException);

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> awsSesEmailService.sendOtpEmail("unverified@example.com", "123456"));

        assertTrue(thrown.getMessage().contains("Failed to send OTP email via AWS SES"));
        assertTrue(thrown.getMessage().contains("Email address is not verified in SES sandbox"));
        assertEquals(sesException, thrown.getCause());
    }

    @Test
    @DisplayName("sendOtpEmail: Handles unexpected exception and translates to BusinessException")
    void testSendOtpEmail_GenericException() {
        when(sesV2Client.sendEmail(any(SendEmailRequest.class))).thenThrow(new RuntimeException("Connection timed out"));

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> awsSesEmailService.sendOtpEmail("user@example.com", "123456"));

        assertTrue(thrown.getMessage().contains("Failed to send password reset email due to an unexpected error."));
    }
}
