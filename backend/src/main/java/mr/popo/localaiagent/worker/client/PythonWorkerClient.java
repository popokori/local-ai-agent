package mr.popo.localaiagent.worker.client;

import mr.popo.localaiagent.worker.dto.EmbedResponse;
import mr.popo.localaiagent.worker.dto.ParseResponse;
import mr.popo.localaiagent.worker.dto.RunCodeResponse;
import mr.popo.localaiagent.worker.dto.WorkerHealth;

import java.util.List;

/**
 * Client vers le worker Python (FastAPI). Le worker héberge :
 *   - modèle d'embeddings BGE-M3 (1024 dim, multilingue FR/EN)
 *   - parsing PDF / DOCX / TXT
 *   - exécution de code Python sandboxée (Phase 3)
 *   - (Phase 5) OCR
 * <p>
 * Le backend ne fait JAMAIS d'embedding, de parsing ni d'exécution de code en JVM
 * — tout passe ici. Auth interne par header {@code X-Internal-Token}.
 */
public interface PythonWorkerClient {

    WorkerHealth health();

    EmbedResponse embed(List<String> texts, String model);

    ParseResponse parse(byte[] fileBytes, String fileName, String mimeType);

    RunCodeResponse runCode(String code, Double timeoutSec);
}
