package mr.popo.localaiagent.worker.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.worker")
public class WorkerProperties {

    @NotBlank
    private String baseUrl = "http://localhost:9000";

    /** Token partagé entre backend et worker (header X-Internal-Token). */
    private String internalToken = "";

    private int timeoutSeconds = 300; // parse PDF gros = long
}
