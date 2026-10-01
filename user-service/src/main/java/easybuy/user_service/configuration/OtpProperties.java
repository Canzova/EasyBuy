package easybuy.user_service.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "otp")
@Getter
@Setter
public class OtpProperties {
    /**
     * Duration in minutes before the generated OTP expires.
     */
    private int expirationMinutes = 5;

    /**
     * Duration in minutes before the temporary reset token expires after OTP verification.
     */
    private int resetTokenExpirationMinutes = 15;
}
