package mr.popo.localaiagent.worker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmbedResponse(
        String model,
        int dim,
        List<List<Float>> embeddings
) {}
