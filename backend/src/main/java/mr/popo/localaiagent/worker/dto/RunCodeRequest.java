package mr.popo.localaiagent.worker.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RunCodeRequest(String code, Double timeoutSec) {}
