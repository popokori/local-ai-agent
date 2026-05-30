package mr.popo.localaiagent.tools.registry;

import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.tools.api.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Découvre tous les beans {@link Tool} disponibles. Vide en Phase 1 (aucune
 * impl de Tool n'est encore enregistrée). Sera peuplé en Phase 3.
 */
@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, Tool> byName;

    public ToolRegistry(List<Tool> tools) {
        this.byName = tools.stream().collect(Collectors.toMap(Tool::name, t -> t));
        log.info("ToolRegistry initialized with {} tool(s): {}", tools.size(), byName.keySet());
    }

    public Optional<Tool> find(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    public List<Tool> all() {
        return List.copyOf(byName.values());
    }
}
