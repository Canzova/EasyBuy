package easybuy.user_service.service.email;

import com.easybuy.common.exceptions.customException.BusinessException;
import easybuy.user_service.configuration.ResendProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Resend implementation of {@link EmailService}.
 * Dispatches transactional emails via Resend's REST API using Spring's RestClient.
 */
@Service
@ConditionalOnProperty(name = "mail.provider", havingValue = "resend", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ResendEmailService implements EmailService {

    private final ResendProperties resendProperties;
    private final RestClient restClient = RestClient.create();

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        if (resendProperties.getApiKey() == null || resendProperties.getApiKey().isBlank()) {
            log.error("Resend API key is not configured (property: resend.api-key). Cannot send OTP email.");
            throw new BusinessException("Email service is temporarily unavailable. Please contact support or try again later.");
        }

        String htmlBody = buildOtpHtmlTemplate(otp);

        Map<String, Object> requestPayload = Map.of(
                "from", resendProperties.getFromEmail(),
                "to", List.of(toEmail),
                "subject", "EasyBuy - Password Reset OTP",
                "html", htmlBody
        );

        try {
            log.info("Dispatching password reset OTP email via Resend to: {}", toEmail);

            restClient.post()
                    .uri(resendProperties.getApiUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully dispatched OTP email via Resend to {}", toEmail);
        } catch (RestClientResponseException e) {
            log.error("Resend API error [status: {}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("Failed to send OTP email via Resend: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Unexpected error while sending email via Resend to {}", toEmail, e);
            throw new BusinessException("Failed to send password reset email due to an unexpected error.", e);
        }
    }

    private String buildOtpHtmlTemplate(String otp) {
        return EmailTemplateHelper.buildOtpHtmlTemplate(otp);
    }
}
