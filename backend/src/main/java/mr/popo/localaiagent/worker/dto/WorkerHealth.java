package mr.popo.localaiagent.worker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkerHealth(
        String status,
        String embeddingModel,
        Integer embeddingDim,
        Long uptimeSeconds
) {}
