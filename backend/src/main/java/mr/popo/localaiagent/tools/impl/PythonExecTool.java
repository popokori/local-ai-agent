package mr.popo.localaiagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.tools.api.Tool;
import mr.popo.localaiagent.tools.api.ToolContext;
import mr.popo.localaiagent.tools.api.ToolResult;
import mr.popo.localaiagent.worker.client.PythonWorkerClient;
import mr.popo.localaiagent.worker.dto.RunCodeResponse;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exécute du code Python via le worker (subprocess + timeout).
 * Utile pour calculs complexes (numpy, sympy), parsing, manipulation de données.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PythonExecTool implements Tool {

    private final PythonWorkerClient workerClient;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "python_exec";
    }

    @Override
    public String description() {
        return "Exécute du code Python (stdout capturé). Timeout 8s par défaut. "
                + "Utilise print(...) pour renvoyer un résultat. "
                + "Bibliothèques disponibles : stdlib complète (math, json, re, datetime, "
                + "urllib, etc.) + requests, numpy, pandas. "
                + "Pas de pip install possible — n'utilise que ces libs. "
                + "Exemple : print(sum(range(100))).";
    }

    @Override
    public JsonNode jsonSchema() {
        ObjectNode code = objectMapper.createObjectNode()
                .put("type", "string")
                .put("description", "Code Python à exécuter (le résultat doit être imprimé via print).");
        ObjectNode timeout = objectMapper.createObjectNode()
                .put("type", "number")
                .put("description", "Timeout en secondes (0.5–30, défaut 8).");
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("code", code);
        properties.set("timeout_sec", timeout);
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", properties);
        schema.set("required", objectMapper.createArrayNode().add("code"));
        return schema;
    }

    @Override
    public ToolResult execute(JsonNode args, ToolContext ctx) {
        String code = args.path("code").asText("");
        if (code.isBlank()) return ToolResult.error("missing 'code'");
        Double timeout = args.has("timeout_sec") && !args.path("timeout_sec").isNull()
                ? args.path("timeout_sec").asDouble()
                : null;
        try {
            RunCodeResponse resp = workerClient.runCode(code, timeout);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("stdout", resp.stdout());
            data.put("stderr", resp.stderr());
            data.put("exitCode", resp.exitCode());
            data.put("timedOut", resp.timedOut());
            String summary;
            if (resp.timedOut()) {
                summary = "timeout";
            } else if (resp.exitCode() != 0) {
                summary = "exit " + resp.exitCode() + " — " + abbreviate(resp.stderr(), 200);
            } else {
                summary = abbreviate(resp.stdout(), 200);
            }
            return ToolResult.ok(summary, data);
        } catch (Exception ex) {
            log.warn("python_exec failed: {}", ex.getMessage());
            return ToolResult.error(ex.getMessage());
        }
    }

    private static String abbreviate(String s, int max) {
        if (s == null) return "";
        s = s.strip();
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
