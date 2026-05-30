package mr.popo.localaiagent.document.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Découpage texte → chunks. Stratégie simple en Phase 2 : fenêtre de
 * {@code chunkSize} caractères avec recouvrement {@code overlap}, en
 * cherchant à couper sur un séparateur naturel (point, saut de ligne).
 * <p>
 * Phase 3+ : passer à un split sémantique (sentence-splitter via worker).
 */
@Component
public class TextChunker {

    private final int chunkSize;
    private final int overlap;

    public TextChunker(@Value("${app.rag.chunk-size-chars:1200}") int chunkSize,
                       @Value("${app.rag.chunk-overlap-chars:200}") int overlap) {
        this.chunkSize = Math.max(200, chunkSize);
        this.overlap = Math.max(0, Math.min(overlap, this.chunkSize / 2));
    }

    /** Découpe un texte de page en chunks. */
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) return List.of();
        String t = text.replaceAll("\\s+", " ").trim();
        if (t.length() <= chunkSize) return List.of(t);

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < t.length()) {
            int end = Math.min(start + chunkSize, t.length());
            if (end < t.length()) {
                // Cherche un séparateur propre dans la dernière fenêtre
                int cut = findNiceCut(t, start + chunkSize - 200, end);
                if (cut > start) end = cut;
            }
            chunks.add(t.substring(start, end).trim());
            if (end >= t.length()) break;
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    private static int findNiceCut(String s, int from, int to) {
        from = Math.max(0, from);
        to = Math.min(s.length(), to);
        for (int i = to - 1; i >= from; i--) {
            char c = s.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                return i + 1;
            }
        }
        return to;
    }
}
