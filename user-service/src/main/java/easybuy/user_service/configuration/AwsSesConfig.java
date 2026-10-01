package easybuy.user_service.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
@ConditionalOnProperty(name = "mail.provider", havingValue = "aws-ses")
@RequiredArgsConstructor
@Slf4j
public class AwsSesConfig {

    private final AwsSesProperties awsSesProperties;

    @Bean
    public SesV2Client sesV2Client() {
        AwsCredentialsProvider credentialsProvider;

        boolean hasStaticKeys = awsSesProperties.getAccessKeyId() != null 
                && !awsSesProperties.getAccessKeyId().isBlank()
                && !awsSesProperties.getAccessKeyId().startsWith("YOUR_")
                && awsSesProperties.getSecretAccessKey() != null 
                && !awsSesProperties.getSecretAccessKey().isBlank()
                && !awsSesProperties.getSecretAccessKey().startsWith("YOUR_");

        if (hasStaticKeys) {
            log.info("[AWS SES] Initializing SesV2Client using static credentials from properties for region: {}", awsSesProperties.getRegion());
            credentialsProvider = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(awsSesProperties.getAccessKeyId(), awsSesProperties.getSecretAccessKey())
            );
        } else {
            log.info("[AWS SES] No static credentials provided in properties. Falling back to DefaultCredentialsProvider (IAM Role / IRSA / Env / ~/.aws) for region: {}", awsSesProperties.getRegion());
            credentialsProvider = DefaultCredentialsProvider.create();
        }

        return SesV2Client.builder()
                .region(Region.of(awsSesProperties.getRegion()))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
