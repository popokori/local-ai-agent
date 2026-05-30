package mr.popo.localaiagent.tools.api;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Phase 3 — outils appelables par l'agent (rag_search, web_search, calculator, etc.).
 * Tous les beans implémentant cette interface seront découverts par {@link mr.popo.localaiagent.tools.registry.ToolRegistry}.
 */
public interface Tool {

    String name();

    String description();

    /** JSON Schema des paramètres (format OpenAI function calling). */
    JsonNode jsonSchema();

    ToolResult execute(JsonNode args, ToolContext context);
}
