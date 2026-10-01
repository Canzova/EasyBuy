package easybuy.user_service.service.email;

import com.easybuy.common.exceptions.customException.BusinessException;
import easybuy.user_service.configuration.AwsSesProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

/**
 * AWS Simple Email Service (SES v2) implementation of {@link EmailService}.
 * Activated by setting mail.provider=aws-ses in configuration.
 *
 * Dispatches password reset transactional OTP emails via AWS SES v2 API.
 */
@Service
@ConditionalOnProperty(name = "mail.provider", havingValue = "aws-ses")
@RequiredArgsConstructor
@Slf4j
public class AwsSesEmailService implements EmailService {

    private final SesV2Client sesV2Client;
    private final AwsSesProperties awsSesProperties;

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        log.info("[AWS SES] Preparing to dispatch password reset OTP email to {}", toEmail);

        try {
            String htmlBody = EmailTemplateHelper.buildOtpHtmlTemplate(otp);

            SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                    .fromEmailAddress(awsSesProperties.getFromEmail())
                    .destination(Destination.builder()
                            .toAddresses(toEmail)
                            .build())
                    .content(EmailContent.builder()
                            .simple(Message.builder()
                                    .subject(Content.builder()
                                            .data("EasyBuy - Password Reset OTP")
                                            .charset("UTF-8")
                                            .build())
                                    .body(Body.builder()
                                            .html(Content.builder()
                                                    .data(htmlBody)
                                                    .charset("UTF-8")
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesV2Client.sendEmail(sendEmailRequest);
            log.info("[AWS SES] Successfully dispatched OTP email to {}. SES Message ID: {}", toEmail, response.messageId());
        } catch (SesV2Exception e) {
            String errorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "UnknownError";
            String errorMessage = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            log.error("[AWS SES] AWS SES service error [status: {}, code: {}]: {}", e.statusCode(), errorCode, errorMessage, e);
            throw new BusinessException("Failed to send OTP email via AWS SES: " + errorMessage, e);
        } catch (Exception e) {
            log.error("[AWS SES] Unexpected error while sending OTP email via AWS SES to {}", toEmail, e);
            throw new BusinessException("Failed to send password reset email due to an unexpected error.", e);
        }
    }
}
