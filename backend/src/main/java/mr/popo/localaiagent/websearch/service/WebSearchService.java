package mr.popo.localaiagent.websearch.service;

import mr.popo.localaiagent.websearch.dto.SearchResult;
import mr.popo.localaiagent.websearch.dto.WebPageContent;

import java.util.List;

/**
 * Recherche web. Phase 3 livre une impl DuckDuckGo (scraping HTML).
 * Impls futures envisagées sans changer l'API :
 *   - SearxngWebSearchService (cluster SearXNG self-hosted via Docker)
 *   - BraveSearchService     (Brave Search API, gratuit ~2000/mois)
 */
public interface WebSearchService {

    /** Renvoie au plus {@code max} résultats. Implémentations : best-effort, peuvent renvoyer moins. */
    List<SearchResult> search(String query, int max);

    /** Télécharge une URL et extrait le texte lisible. */
    WebPageContent fetch(String url);
}
