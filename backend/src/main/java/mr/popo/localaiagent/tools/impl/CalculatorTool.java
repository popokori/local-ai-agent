package mr.popo.localaiagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.tools.api.Tool;
import mr.popo.localaiagent.tools.api.ToolContext;
import mr.popo.localaiagent.tools.api.ToolResult;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Évaluation d'expressions mathématiques via exp4j.
 * Fonctions supportées : +, -, *, /, ^, sqrt, sin, cos, tan, log, ln, exp, abs.
 * Constantes : pi, e.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CalculatorTool implements Tool {

    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "calculator";
    }

    @Override
    public String description() {
        return "Évalue une expression mathématique. Supporte +, -, *, /, ^, sqrt, sin, cos, tan, log, ln, exp, abs, pi, e. Exemple : sqrt(2) * 17 ou 17 * 23.";
    }

    @Override
    public JsonNode jsonSchema() {
        ObjectNode expression = objectMapper.createObjectNode()
                .put("type", "string")
                .put("description", "Expression à évaluer.");
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("expression", expression);
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", properties);
        schema.set("required", objectMapper.createArrayNode().add("expression"));
        return schema;
    }

    @Override
    public ToolResult execute(JsonNode args, ToolContext ctx) {
        String expr = args.path("expression").asText("");
        if (expr.isBlank()) return ToolResult.error("missing 'expression'");
        try {
            Expression e = new ExpressionBuilder(expr).build();
            double value = e.evaluate();
            String formatted = (value == Math.rint(value) && !Double.isInfinite(value))
                    ? String.valueOf((long) value)
                    : String.valueOf(value);
            return ToolResult.ok(expr + " = " + formatted,
                    Map.of("expression", expr, "value", value, "formatted", formatted));
        } catch (Exception ex) {
            log.debug("calculator failed for '{}': {}", expr, ex.getMessage());
            return ToolResult.error("invalid expression: " + ex.getMessage());
        }
    }
}
