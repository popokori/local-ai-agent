package mr.popo.localaiagent.worker.client;

import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.worker.config.WorkerProperties;
import mr.popo.localaiagent.worker.dto.EmbedRequest;
import mr.popo.localaiagent.worker.dto.EmbedResponse;
import mr.popo.localaiagent.worker.dto.ParseResponse;
import mr.popo.localaiagent.worker.dto.RunCodeRequest;
import mr.popo.localaiagent.worker.dto.RunCodeResponse;
import mr.popo.localaiagent.worker.dto.WorkerHealth;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class HttpPythonWorkerClient implements PythonWorkerClient {

    private static final String INTERNAL_HEADER = "X-Internal-Token";

    private final WebClient web;
    private final WorkerProperties props;

    public HttpPythonWorkerClient(WebClient.Builder builder, WorkerProperties props) {
        this.props = props;
        WebClient.Builder b = builder.clone().baseUrl(props.getBaseUrl());
        if (props.getInternalToken() != null && !props.getInternalToken().isBlank()) {
            b.defaultHeader(INTERNAL_HEADER, props.getInternalToken());
        }
        this.web = b.build();
    }

    @Override
    public WorkerHealth health() {
        return web.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(WorkerHealth.class)
                .timeout(Duration.ofSeconds(5))
                .block();
    }

    @Override
    public EmbedResponse embed(List<String> texts, String model) {
        return web.post()
                .uri("/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new EmbedRequest(texts, model))
                .retrieve()
                .bodyToMono(EmbedResponse.class)
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .block();
    }

    @Override
    public RunCodeResponse runCode(String code, Double timeoutSec) {
        return web.post()
                .uri("/run-code")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RunCodeRequest(code, timeoutSec))
                .retrieve()
                .bodyToMono(RunCodeResponse.class)
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .block();
    }

    @Override
    public ParseResponse parse(byte[] fileBytes, String fileName, String mimeType) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", new ByteArrayResource(fileBytes) {
            @Override public String getFilename() { return fileName; }
        }).header(HttpHeaders.CONTENT_TYPE, mimeType != null ? mimeType : MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return web.post()
                .uri("/parse")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body.build()))
                .retrieve()
                .bodyToMono(ParseResponse.class)
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .block();
    }
}
