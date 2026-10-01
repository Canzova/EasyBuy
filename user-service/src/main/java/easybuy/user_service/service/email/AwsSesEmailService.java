package easybuy.user_service.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * AWS Simple Email Service (SES) implementation of {@link EmailService}.
 * Activated by setting mail.provider=aws-ses in configuration.
 *
 * Designed for seamless swap when replacing Resend with AWS SES.
 */
@Service
@ConditionalOnProperty(name = "mail.provider", havingValue = "aws-ses")
@Slf4j
public class AwsSesEmailService implements EmailService {

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        log.info("[AWS SES] Preparing to dispatch password reset OTP email to {}", toEmail);
        // When migrating to AWS SES:
        // 1. Add dependency: software.amazon.awssdk:ses (or sesv2)
        // 2. Configure SesClient bean with AWS Region and credentials
        // 3. Send email using sesClient.sendEmail(...)
        log.warn("[AWS SES] AWS SES driver is activated but requires AWS SDK client configuration. OTP: {}", otp);
        throw new UnsupportedOperationException("AWS SES Email service is configured as the active provider, but AWS credentials/SesClient are pending setup.");
    }
}
