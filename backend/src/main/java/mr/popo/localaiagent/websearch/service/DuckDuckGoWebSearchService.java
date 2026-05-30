package mr.popo.localaiagent.websearch.service;

import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.common.exception.BusinessException;
import mr.popo.localaiagent.websearch.dto.SearchResult;
import mr.popo.localaiagent.websearch.dto.WebPageContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Recherche web par scraping de l'interface HTML de DuckDuckGo
 * ({@code https://html.duckduckgo.com/html/}) — zéro install, zéro API key.
 * <p>
 * Limites connues :
 *   - DDG peut renvoyer une page anti-bot si trop de requêtes.
 *   - Sélecteurs CSS sensibles aux changements DDG.
 * Une vraie production utiliserait SearXNG (Docker) ou Brave Search API.
 */
@Slf4j
@Service
public class DuckDuckGoWebSearchService implements WebSearchService {

    private static final String SEARCH_URL = "https://html.duckduckgo.com/html/";
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0 Safari/537.36";
    private static final int TIMEOUT_MS = 12_000;
    private static final int MAX_FETCH_CHARS = 6_000;

    @Override
    public List<SearchResult> search(String query, int max) {
        if (query == null || query.isBlank()) return List.of();
        int target = Math.max(1, Math.min(max, 10));
        try {
            Document doc = Jsoup.connect(SEARCH_URL)
                    .userAgent(UA)
                    .timeout(TIMEOUT_MS)
                    .data("q", query)
                    .data("kl", "wt-wt")
                    .post();

            List<SearchResult> results = new ArrayList<>();
            Elements blocks = doc.select("div.result");
            for (Element block : blocks) {
                Element titleEl = block.selectFirst("a.result__a");
                if (titleEl == null) continue;
                String href = decodeDdgRedirect(titleEl.attr("href"));
                if (href.isBlank()) continue;
                String title = titleEl.text();
                Element snippetEl = block.selectFirst("a.result__snippet, div.result__snippet");
                String snippet = snippetEl != null ? snippetEl.text() : "";
                results.add(new SearchResult(title, href, snippet));
                if (results.size() >= target) break;
            }
            log.debug("DDG search '{}' → {} results", query, results.size());
            return results;
        } catch (IOException ex) {
            log.warn("DDG search failed: {}", ex.getMessage());
            throw new BusinessException("Web search failed: " + ex.getMessage());
        }
    }

    @Override
    public WebPageContent fetch(String url) {
        if (url == null || url.isBlank()) throw new BusinessException("empty url");
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(UA)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .maxBodySize(2_000_000) // 2 Mo de HTML max
                    .get();
            String title = doc.title();
            // Supprime script/style/nav avant extraction
            doc.select("script, style, nav, footer, header, noscript, svg").remove();
            String text = doc.body() != null ? doc.body().text() : doc.text();
            int originalLen = text.length();
            if (text.length() > MAX_FETCH_CHARS) {
                text = text.substring(0, MAX_FETCH_CHARS) + "\n[content truncated]";
            }
            return new WebPageContent(url, title, text, originalLen);
        } catch (IOException ex) {
            log.warn("Web fetch '{}' failed: {}", url, ex.getMessage());
            throw new BusinessException("Web fetch failed: " + ex.getMessage());
        }
    }

    /**
     * DuckDuckGo réécrit les liens en {@code //duckduckgo.com/l/?uddg=ENCODED_URL}
     * pour tracker les clics. On décode pour récupérer l'URL réelle.
     */
    private static String decodeDdgRedirect(String href) {
        if (href.startsWith("//")) href = "https:" + href;
        try {
            URI uri = URI.create(href);
            String query = uri.getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("uddg=")) {
                        return URLDecoder.decode(param.substring(5), StandardCharsets.UTF_8);
                    }
                }
            }
            return href;
        } catch (Exception ex) {
            return href;
        }
    }
}
