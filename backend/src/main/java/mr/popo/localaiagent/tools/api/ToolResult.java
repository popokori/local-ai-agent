package mr.popo.localaiagent.tools.api;

public record ToolResult(boolean success, String summary, Object data) {

    public static ToolResult ok(String summary, Object data) {
        return new ToolResult(true, summary, data);
    }

    public static ToolResult error(String message) {
        return new ToolResult(false, message, null);
    }
}
