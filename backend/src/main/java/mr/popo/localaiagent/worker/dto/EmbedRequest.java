package mr.popo.localaiagent.worker.dto;

import java.util.List;

public record EmbedRequest(List<String> texts, String model) {}
