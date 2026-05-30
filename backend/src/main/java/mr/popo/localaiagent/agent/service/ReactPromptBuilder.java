package mr.popo.localaiagent.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mr.popo.localaiagent.tools.api.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Construit la portion du system prompt qui décrit le protocole ReAct + le
 * catalogue d'outils avec leurs schémas JSON. Inclure les schémas explicitement
 * évite que des modèles comme Llama 3.1 8B inventent des noms d'arguments.
 */
@Component
@RequiredArgsConstructor
public class ReactPromptBuilder {

    private final ObjectMapper objectMapper;

    public String build(List<Tool> tools) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n## Protocole de raisonnement (ReAct)\n\n");
        sb.append("Tu as accès aux outils suivants :\n\n");
        for (Tool t : tools) {
            sb.append("### ").append(t.name()).append("\n");
            sb.append(t.description()).append("\n");
            sb.append("Schéma JSON des arguments : ").append(stringify(t.jsonSchema())).append("\n\n");
        }
        sb.append("""
                Pour chaque question, tu réponds en UNE SEULE étape à la fois, en suivant
                STRICTEMENT ce format. ARRÊTE-TOI après "Action Input:" pour laisser le
                système exécuter l'outil. N'invente JAMAIS l'Observation.

                Thought: <réflexion courte>
                Action: <nom exact d'un outil>
                Action Input: <JSON valide des arguments, ex. {"expression": "17*23"}>

                Le système t'enverra ensuite :
                Observation: <résultat>

                Tu peux enchaîner plusieurs cycles (max 5). Quand tu as tout ce qu'il faut :

                Thought: J'ai assez d'éléments.
                Final Answer: <réponse complète à l'utilisateur, en français par défaut>

                Règles non négociables :
                - Si aucun outil n'est utile, va directement à "Final Answer:".
                - Le JSON dans Action Input DOIT utiliser les noms d'arguments exacts du schéma.
                - Cite tes sources si tu as obtenu des données via web_search, web_fetch ou rag_search.
                """);
        return sb.toString();
    }

    private String stringify(JsonNode schema) {
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
