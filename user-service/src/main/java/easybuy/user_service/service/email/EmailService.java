package easybuy.user_service.service.email;

/**
 * Service interface for sending emails.
 * Enables interchangeable providers (e.g. Resend, AWS SES, SMTP).
 */
public interface EmailService {

    /**
     * Sends an email containing a One-Time Password (OTP) for password reset.
     *
     * @param toEmail recipient email address
     * @param otp     the 6-digit one-time password
     */
    void sendOtpEmail(String toEmail, String otp);
}
