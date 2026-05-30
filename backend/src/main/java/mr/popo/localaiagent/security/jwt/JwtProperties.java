package mr.popo.localaiagent.security.jwt;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Arrays;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    @NotBlank
    private String secret;

    private Duration accessTtl = Duration.ofMinutes(15);

    private Duration refreshTtl = Duration.ofDays(7);

    private String issuer = "localaiagent";

    private final Environment environment;

    public JwtProperties(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (prod && secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters in 'prod' profile (current: " + secret.length() + ")");
        }
    }
}
