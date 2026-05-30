package mr.popo.localaiagent.agent.dto;

import com.fasterxml.jackson.databind.JsonNode;
import mr.popo.localaiagent.rag.dto.RagHit;

import java.util.List;

/**
 * Événements émis par l'agent vers le streaming SSE. Sealed pour exhaustivité
 * pattern-matching côté consumer.
 */
public sealed interface AgentEvent {

    /** Token textuel à pousser au client. */
    record Token(String text) implements AgentEvent {}

    /** Sources documentaires retournées par le RAG. */
    record Sources(List<RagHit> hits) implements AgentEvent {}

    /** Annonce d'un appel d'outil par l'agent. */
    record ToolStart(int iteration, String toolName, JsonNode arguments) implements AgentEvent {}

    /** Résultat d'un appel d'outil. */
    record ToolEnd(int iteration, String toolName, boolean success, String summary) implements AgentEvent {}

    /** Fin de génération (finish_reason renseigné). */
    record End(String finishReason) implements AgentEvent {}

    /** Erreur non récupérable. */
    record Error(String message) implements AgentEvent {}
}
