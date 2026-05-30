package mr.popo.localaiagent.tools.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import mr.popo.localaiagent.tools.api.Tool;
import mr.popo.localaiagent.tools.api.ToolContext;
import mr.popo.localaiagent.tools.api.ToolResult;
import mr.popo.localaiagent.tools.registry.ToolRegistry;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ToolDispatcher {

    private final ToolRegistry registry;

    public ToolResult dispatch(String toolName, JsonNode args, ToolContext ctx) {
        Tool tool = registry.find(toolName)
                .orElseThrow(() -> new UnsupportedOperationException(
                        "Tool '" + toolName + "' not available — Phase 1 ships with no tools enabled."));
        return tool.execute(args, ctx);
    }
}
