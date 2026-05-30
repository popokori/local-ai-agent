package mr.popo.localaiagent.science.service;

import mr.popo.localaiagent.science.api.Domain;
import mr.popo.localaiagent.science.api.ScientificRouter;
import org.springframework.stereotype.Component;

/**
 * Phase 1 — router neutre : tout est GENERIC. Sera remplacé en Phase 4.
 */
@Component
public class NoopScientificRouter implements ScientificRouter {

    @Override
    public Domain detect(String userMessage) {
        return Domain.GENERIC;
    }
}
