package mr.popo.localaiagent.rag.service;

import mr.popo.localaiagent.rag.dto.RagAnswer;

/**
 * Retrieval-Augmented Generation : interroge la KB d'un utilisateur et
 * renvoie le contexte pertinent à injecter dans le prompt LLM.
 */
public interface RagService {

    /**
     * Recherche les top-K passages les plus pertinents pour la requête,
     * dans la KB {@code kbId} appartenant à {@code ownerId}.
     */
    RagAnswer query(Long ownerId, Long kbId, String query, int topK);
}
