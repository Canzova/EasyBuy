package easybuy.user_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity  // TO enable spring security auto-configuration including  — including the AuthenticationConfiguration bean.
public class SecurityConfig {

    /*
        Here we are just disabling the default behavior of spring security and we are saying disable all the security in all endpoints.
     */

    @Bean
    public SecurityFilterChain customSpringSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorizeRequests ->
                            authorizeRequests.anyRequest().permitAll()
                ).build();

    }
}
