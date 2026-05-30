package mr.popo.localaiagent.streaming.dto;

/**
 * Types d'événements SSE émis vers le client. Phase 1 utilise uniquement
 * TOKEN, FINAL, ERROR. Les autres types sont réservés pour Phase 2/3.
 */
public enum StreamEventType {
    TOKEN,
    FINAL,
    ERROR,
    // Phase 2+ :
    TOOL_START,
    TOOL_END,
    SOURCE
}
