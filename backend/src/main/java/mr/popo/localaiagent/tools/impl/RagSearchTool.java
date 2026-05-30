package mr.popo.localaiagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.rag.dto.RagAnswer;
import mr.popo.localaiagent.rag.dto.RagHit;
import mr.popo.localaiagent.rag.service.RagService;
import mr.popo.localaiagent.tools.api.Tool;
import mr.popo.localaiagent.tools.api.ToolContext;
import mr.popo.localaiagent.tools.api.ToolResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outil RAG appelable par l'agent (Phase 3+ : via function calling).
 * En Phase 2, le RAG est aussi appelé automatiquement dans {@link mr.popo.localaiagent.agent.service.AgentService}
 * si la session a une KB attachée. Cet outil expose la même capacité mais
 * sera utilisé par la boucle ReAct quand on l'activera.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagSearchTool implements Tool {

    private final RagService ragService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "rag_search";
    }

    @Override
    public String description() {
        return "Recherche des passages pertinents dans la base documentaire de l'utilisateur. "
                + "Renvoie jusqu'à topK passages avec score, nom de document et numéro de page. "
                + "Utilise cet outil pour répondre à toute question pouvant être documentée.";
    }

    @Override
    public JsonNode jsonSchema() {
        var query = objectMapper.createObjectNode()
                .put("type", "string")
                .put("description", "La question reformulée pour la recherche.");
        var kbId = objectMapper.createObjectNode()
                .put("type", "integer")
                .put("description", "ID de la knowledge base à interroger.");
        var topK = objectMapper.createObjectNode()
                .put("type", "integer")
                .put("description", "Nombre maximum de passages à retourner (défaut 5).");
        var properties = objectMapper.createObjectNode();
        properties.set("query", query);
        properties.set("kb_id", kbId);
        properties.set("top_k", topK);
        var schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", properties);
        schema.set("required", objectMapper.createArrayNode().add("query").add("kb_id"));
        return schema;
    }

    @Override
    public ToolResult execute(JsonNode args, ToolContext ctx) {
        String query = args.path("query").asText("");
        long kbId = args.path("kb_id").asLong(0);
        int topK = args.path("top_k").asInt(5);

        if (query.isBlank() || kbId <= 0) {
            return ToolResult.error("missing 'query' or 'kb_id'");
        }
        try {
            RagAnswer answer = ragService.query(ctx.userId(), kbId, query, topK);
            List<RagHit> hits = answer.hits();
            return ToolResult.ok(
                    hits.isEmpty() ? "no result" : hits.size() + " passage(s) found",
                    answer);
        } catch (Exception ex) {
            log.warn("rag_search failed for kb={} user={}: {}", kbId, ctx.userId(), ex.getMessage());
            return ToolResult.error(ex.getMessage());
        }
    }
}
