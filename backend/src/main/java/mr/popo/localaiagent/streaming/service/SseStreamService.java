package mr.popo.localaiagent.streaming.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.streaming.dto.StreamEventType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Locale;

/**
 * Helpers de sérialisation pour {@link SseEmitter}. Garde le format
 *   event: <type>
 *   data:  <json>
 * uniformisé partout dans le backend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseStreamService {

    private final ObjectMapper objectMapper;

    public void send(SseEmitter emitter, StreamEventType type, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(type.name().toLowerCase(Locale.ROOT))
                    .data(objectMapper.writeValueAsString(data)));
        } catch (IOException ex) {
            log.debug("SSE send failed (client likely disconnected): {}", ex.getMessage());
            emitter.completeWithError(ex);
        }
    }

    public void sendHeartbeat(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("ping"));
        } catch (IOException ex) {
            log.debug("SSE heartbeat failed: {}", ex.getMessage());
        }
    }
}
