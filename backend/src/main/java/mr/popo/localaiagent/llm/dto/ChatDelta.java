package mr.popo.localaiagent.llm.dto;

/**
 * Fragment de réponse parsé depuis un chunk SSE OpenAI-compatible.
 * <p>
 * `content` est le texte (delta) à émettre vers le client. `finishReason` est
 * non-null sur le dernier chunk ("stop", "length", "tool_calls" en Phase 2+).
 */
public record ChatDelta(String content, String finishReason) {

    public static ChatDelta token(String content) {
        return new ChatDelta(content, null);
    }

    public static ChatDelta end(String reason) {
        return new ChatDelta(null, reason);
    }

    public boolean isEnd() {
        return finishReason != null;
    }
}
