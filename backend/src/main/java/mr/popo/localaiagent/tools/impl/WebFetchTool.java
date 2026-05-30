package mr.popo.localaiagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.tools.api.Tool;
import mr.popo.localaiagent.tools.api.ToolContext;
import mr.popo.localaiagent.tools.api.ToolResult;
import mr.popo.localaiagent.websearch.dto.WebPageContent;
import mr.popo.localaiagent.websearch.service.WebSearchService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebFetchTool implements Tool {

    private final WebSearchService webSearchService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "web_fetch";
    }

    @Override
    public String description() {
        return "Télécharge le contenu lisible (titre + texte) d'une URL. Utilise après web_search quand l'extrait est insuffisant.";
    }

    @Override
    public JsonNode jsonSchema() {
        ObjectNode url = objectMapper.createObjectNode()
                .put("type", "string")
                .put("description", "URL absolue à récupérer.");
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("url", url);
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", properties);
        schema.set("required", objectMapper.createArrayNode().add("url"));
        return schema;
    }

    @Override
    public ToolResult execute(JsonNode args, ToolContext ctx) {
        String url = args.path("url").asText("");
        if (url.isBlank()) return ToolResult.error("missing 'url'");
        try {
            WebPageContent page = webSearchService.fetch(url);
            return ToolResult.ok("Fetched " + url + " (" + page.bytes() + " bytes)",
                    Map.of("url", page.url(), "title", page.title(), "text", page.text()));
        } catch (Exception ex) {
            log.warn("web_fetch failed: {}", ex.getMessage());
            return ToolResult.error(ex.getMessage());
        }
    }
}
