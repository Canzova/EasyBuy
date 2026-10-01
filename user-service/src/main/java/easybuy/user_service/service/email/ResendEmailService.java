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
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>EasyBuy - Password Reset OTP</title>
              <style>
                body {
                  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                  background-color: #f3f4f6;
                  margin: 0;
                  padding: 24px;
                }
                .email-card {
                  max-width: 480px;
                  margin: 0 auto;
                  background-color: #ffffff;
                  border-radius: 12px;
                  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
                  padding: 36px 32px;
                  border: 1px solid #e5e7eb;
                }
                .brand-title {
                  text-align: center;
                  font-size: 24px;
                  font-weight: 800;
                  color: #111827;
                  letter-spacing: -0.5px;
                  margin-bottom: 24px;
                }
                .brand-title span {
                  color: #3b82f6;
                }
                .greeting {
                  font-size: 16px;
                  color: #374151;
                  margin-bottom: 12px;
                }
                .instructions {
                  font-size: 14px;
                  color: #4b5563;
                  line-height: 1.6;
                  margin-bottom: 24px;
                }
                .otp-box {
                  text-align: center;
                  background-color: #eff6ff;
                  border: 2px dashed #3b82f6;
                  border-radius: 8px;
                  padding: 18px;
                  font-size: 32px;
                  font-weight: 800;
                  letter-spacing: 8px;
                  color: #1d4ed8;
                  margin: 24px 0;
                }
                .warning {
                  font-size: 13px;
                  color: #dc2626;
                  background-color: #fef2f2;
                  border-left: 3px solid #dc2626;
                  padding: 10px 14px;
                  border-radius: 4px;
                  margin-bottom: 20px;
                }
                .footer {
                  text-align: center;
                  font-size: 12px;
                  color: #9ca3af;
                  border-top: 1px solid #f3f4f6;
                  padding-top: 20px;
                  margin-top: 24px;
                }
              </style>
            </head>
            <body>
              <div class="email-card">
                <div class="brand-title">Easy<span>Buy</span></div>
                <div class="greeting">Hello,</div>
                <div class="instructions">
                  You requested to reset the password for your EasyBuy account. Use the following One-Time Password (OTP) to proceed:
                </div>
                <div class="otp-box">""" + otp + """
                </div>
                <div class="warning">
                  This OTP is confidential and expires in 5 minutes. Do not share it with anyone.
                </div>
                <div class="instructions">
                  If you did not request this, please disregard this email. Your password will remain unchanged.
                </div>
                <div class="footer">
                  &copy; EasyBuy Microservices &bull; Automated Security Notification
                </div>
              </div>
            </body>
            </html>
            """;
    }
}
