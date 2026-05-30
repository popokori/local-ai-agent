package mr.popo.localaiagent.worker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RunCodeResponse(
        String stdout,
        String stderr,
        int exitCode,
        boolean timedOut,
        boolean truncated
) {}
