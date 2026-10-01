package easybuy.user_service.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "resend")
@Getter
@Setter
public class ResendProperties {
    /**
     * Resend API Key (starts with re_)
     */
    private String apiKey;

    /**
     * Sender email address (e.g. EasyBuy <onboarding@resend.dev> or verified domain)
     */
    private String fromEmail = "EasyBuy <onboarding@resend.dev>";

    /**
     * Resend API endpoint
     */
    private String apiUrl = "https://api.resend.com/emails";
}
