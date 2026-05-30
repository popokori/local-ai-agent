package mr.popo.localaiagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.tools.api.Tool;
import mr.popo.localaiagent.tools.api.ToolContext;
import mr.popo.localaiagent.tools.api.ToolResult;
import mr.popo.localaiagent.websearch.dto.SearchResult;
import mr.popo.localaiagent.websearch.service.WebSearchService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSearchTool implements Tool {

    private final WebSearchService webSearchService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "web_search";
    }

    @Override
    public String description() {
        return "Recherche sur le web (DuckDuckGo). Retourne titre + URL + extrait pour les N premiers résultats. Utilise cet outil pour des informations factuelles, récentes ou hors de la base documentaire.";
    }

    @Override
    public JsonNode jsonSchema() {
        ObjectNode query = objectMapper.createObjectNode()
                .put("type", "string")
                .put("description", "Requête à envoyer au moteur de recherche.");
        ObjectNode max = objectMapper.createObjectNode()
                .put("type", "integer")
                .put("description", "Nombre maximum de résultats (défaut 5, max 10).");
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("query", query);
        properties.set("max", max);
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", properties);
        schema.set("required", objectMapper.createArrayNode().add("query"));
        return schema;
    }

    @Override
    public ToolResult execute(JsonNode args, ToolContext ctx) {
        String query = args.path("query").asText("");
        int max = args.path("max").asInt(5);
        if (query.isBlank()) return ToolResult.error("missing 'query'");
        try {
            List<SearchResult> results = webSearchService.search(query, max);
            String summary = results.isEmpty()
                    ? "No result"
                    : results.size() + " result(s) for '" + query + "'";
            return ToolResult.ok(summary, Map.of("query", query, "results", results));
        } catch (Exception ex) {
            log.warn("web_search failed: {}", ex.getMessage());
            return ToolResult.error(ex.getMessage());
        }
    }
}
