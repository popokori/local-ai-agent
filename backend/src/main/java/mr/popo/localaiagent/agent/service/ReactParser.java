package mr.popo.localaiagent.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser tolérant des réponses ReAct du LLM.
 * <p>
 * Formats acceptés :
 * <pre>
 *   Thought: ...
 *   Action: tool_name
 *   Action Input: {"arg": "value"}
 *
 *   ou
 *
 *   Thought: ...
 *   Final Answer: la réponse à l'utilisateur
 * </pre>
 * Les "Action Input" peuvent être un JSON ou une string simple (auto-wrappée
 * en {@code {"query": "..."}}).
 */
@Slf4j
public class ReactParser {

    private static final Pattern FINAL_ANSWER = Pattern.compile(
            "Final\\s*Answer\\s*:\\s*(.+?)\\s*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ACTION = Pattern.compile(
            "Action\\s*:\\s*([\\w_-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACTION_INPUT = Pattern.compile(
            "Action\\s*Input\\s*:\\s*(.+?)(?=\\n\\s*(?:Observation|Thought|Action|Final\\s*Answer)\\s*:|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final ObjectMapper objectMapper;

    public ReactParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Décide ce que le LLM nous a demandé de faire. */
    public Decision parse(String llmText) {
        if (llmText == null || llmText.isBlank()) {
            return Decision.finalAnswer("");
        }
        Matcher fa = FINAL_ANSWER.matcher(llmText);
        if (fa.find()) {
            return Decision.finalAnswer(fa.group(1).trim());
        }
        Matcher act = ACTION.matcher(llmText);
        Matcher actIn = ACTION_INPUT.matcher(llmText);
        if (act.find() && actIn.find()) {
            String tool = act.group(1).trim();
            String rawInput = actIn.group(1).trim();
            JsonNode args = parseArgs(rawInput);
            return Decision.toolCall(tool, args);
        }
        // Aucun format ReAct détecté → on traite le tout comme une réponse directe
        return Decision.finalAnswer(llmText.trim());
    }

    private JsonNode parseArgs(String raw) {
        String s = stripCodeFences(raw);
        // JSON direct ?
        try {
            return objectMapper.readTree(s);
        } catch (Exception ignore) {
            // pas du JSON valide → on traite comme une string simple
        }
        ObjectNode node = objectMapper.createObjectNode();
        // Trim quotes encadrants éventuels
        if (s.length() >= 2
                && ((s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
                || (s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\''))) {
            s = s.substring(1, s.length() - 1);
        }
        node.put("query", s);
        node.put("expression", s);
        node.put("url", s);
        node.put("code", s);
        return node;
    }

    private static String stripCodeFences(String s) {
        s = s.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            if (firstNl > 0) s = s.substring(firstNl + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }

    /** Décision parsée depuis la réponse du LLM. */
    public sealed interface Decision {
        record FinalAnswer(String text) implements Decision {}
        record ToolCall(String toolName, JsonNode arguments) implements Decision {}

        static FinalAnswer finalAnswer(String text) { return new FinalAnswer(text); }
        static ToolCall toolCall(String tool, JsonNode args) { return new ToolCall(tool, args); }
    }
}
