package mr.popo.localaiagent.chat.domain;

/**
 * Mode de réponse d'une session. En Phase 1 seul NORMAL est câblé ;
 * EXPERT et FACT_CHECK seront activés en Phase 4.
 */
public enum ChatMode {
    NORMAL,
    EXPERT,
    FACT_CHECK
}
