package mr.popo.localaiagent.science.api;

/**
 * Détection du domaine scientifique d'une question.
 * Phase 1 : impl bouchon {@link mr.popo.localaiagent.science.service.NoopScientificRouter}
 * qui renvoie toujours {@link Domain#GENERIC}.
 * Phase 4 : impl réelle (mots-clés + classifieur LLM, sélection KB cible).
 */
public interface ScientificRouter {

    Domain detect(String userMessage);
}
