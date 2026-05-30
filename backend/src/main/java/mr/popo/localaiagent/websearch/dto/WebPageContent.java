package mr.popo.localaiagent.websearch.dto;

public record WebPageContent(
        String url,
        String title,
        String text,
        int bytes
) {}
