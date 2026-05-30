package mr.popo.localaiagent.rag.dto;

import java.util.List;

/**
 * Résultat structuré d'une requête RAG.
 *
 * @param contextBlock texte concaténé prêt à injecter dans un system prompt,
 *                     avec balises [source N] pour permettre les citations
 * @param hits         passages retournés avec leurs scores et métadonnées
 */
public record RagAnswer(String contextBlock, List<RagHit> hits) {

    public boolean isEmpty() {
        return hits == null || hits.isEmpty();
    }
}
