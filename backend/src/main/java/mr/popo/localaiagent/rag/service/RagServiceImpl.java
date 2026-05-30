package mr.popo.localaiagent.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.common.exception.ResourceNotFoundException;
import mr.popo.localaiagent.document.domain.Document;
import mr.popo.localaiagent.document.domain.KnowledgeBase;
import mr.popo.localaiagent.document.repository.DocumentRepository;
import mr.popo.localaiagent.document.repository.KnowledgeBaseRepository;
import mr.popo.localaiagent.rag.dto.RagAnswer;
import mr.popo.localaiagent.rag.dto.RagHit;
import mr.popo.localaiagent.rag.vector.VectorHit;
import mr.popo.localaiagent.rag.vector.VectorStoreService;
import mr.popo.localaiagent.worker.client.PythonWorkerClient;
import mr.popo.localaiagent.worker.dto.EmbedResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final PythonWorkerClient workerClient;
    private final VectorStoreService vectorStore;
    private final KnowledgeBaseRepository kbRepository;
    private final DocumentRepository documentRepository;

    @Override
    @Transactional(readOnly = true)
    public RagAnswer query(Long ownerId, Long kbId, String query, int topK) {
        KnowledgeBase kb = kbRepository.findByIdAndOwnerId(kbId, ownerId)
                .orElseThrow(() -> ResourceNotFoundException.of("KnowledgeBase", kbId));

        EmbedResponse embed = workerClient.embed(List.of(query), kb.getEmbeddingModel());
        if (embed == null || embed.embeddings() == null || embed.embeddings().isEmpty()) {
            log.warn("Empty embedding response for KB={} query='{}'", kbId, abbreviate(query));
            return new RagAnswer("", List.of());
        }
        float[] queryVec = toFloatArray(embed.embeddings().get(0));

        List<VectorHit> raw = vectorStore.search(ownerId, kbId, queryVec, topK);
        if (raw.isEmpty()) {
            return new RagAnswer("", List.of());
        }

        // Charge les noms des documents en batch pour éviter N+1
        Map<Long, String> docNames = resolveDocumentNames(ownerId, raw);

        List<RagHit> hits = raw.stream()
                .map(h -> new RagHit(h.chunkId(), h.documentId(),
                        docNames.getOrDefault(h.documentId(), "document"),
                        h.pageNumber(), h.text(), h.score()))
                .toList();

        return new RagAnswer(buildContextBlock(hits), hits);
    }

    private Map<Long, String> resolveDocumentNames(Long ownerId, List<VectorHit> hits) {
        Map<Long, String> names = new HashMap<>();
        for (VectorHit h : hits) {
            if (!names.containsKey(h.documentId())) {
                documentRepository.findByIdAndOwnerId(h.documentId(), ownerId)
                        .map(Document::getFileName)
                        .ifPresent(n -> names.put(h.documentId(), n));
            }
        }
        return names;
    }

    private static String buildContextBlock(List<RagHit> hits) {
        StringBuilder sb = new StringBuilder();
        sb.append("Voici des extraits documentaires pertinents pour la question. ")
          .append("Cite la source entre crochets, par ex. [source 1].\n\n");
        for (int i = 0; i < hits.size(); i++) {
            RagHit h = hits.get(i);
            sb.append("[source ").append(i + 1).append("] ")
              .append(h.documentName());
            if (h.pageNumber() != null) sb.append(" — p. ").append(h.pageNumber());
            sb.append("\n").append(h.text()).append("\n\n");
        }
        return sb.toString();
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        return s.length() <= 60 ? s : s.substring(0, 57) + "...";
    }
}
