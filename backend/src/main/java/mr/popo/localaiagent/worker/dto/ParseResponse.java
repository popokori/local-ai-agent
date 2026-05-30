package mr.popo.localaiagent.worker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParseResponse(
        String fileName,
        String mimeType,
        Integer pageCount,
        List<ParsedPage> pages
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ParsedPage(int n, String text) {}
}
