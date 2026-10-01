package easybuy.user_service.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aws.ses")
@Getter
@Setter
public class AwsSesProperties {

    /**
     * AWS Region hosting the SES service (e.g., us-east-1, ap-south-1).
     */
    private String region = "us-east-1";

    /**
     * AWS Access Key ID.
     * When left empty or null, credentials resolve via AWS DefaultCredentialsProvider
     * (IAM Instance Profile, ECS Task Role, EKS IRSA, or local AWS CLI credentials).
     */
    private String accessKeyId;

    /**
     * AWS Secret Access Key.
     */
    private String secretAccessKey;

    /**
     * Sender email address verified in AWS SES (e.g. EasyBuy <noreply@easybuy.com>).
     */
    private String fromEmail = "EasyBuy <noreply@easybuy.com>";
}
