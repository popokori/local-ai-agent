package mr.popo.localaiagent.llm.config;

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
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    /** ollama | vllm — utilisé pour le diagnostic uniquement, le wire format est identique. */
    @NotBlank
    private String provider = "ollama";

    /** URL OpenAI-compatible (avec /v1). */
    @NotBlank
    private String baseUrl;

    /** URL native pour /api/tags (Ollama uniquement). Sert au health check. */
    @NotBlank
    private String nativeBaseUrl;

    private String apiKey = "";

    @NotBlank
    private String defaultModel;

    private String expertModel;

    private String factCheckModel;

    private int timeoutSeconds = 120;

    private int connectTimeoutSeconds = 10;

    private double temperature = 0.3;

    private int maxTokens = 2048;
}
