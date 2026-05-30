package mr.popo.localaiagent.document.service;

import lombok.RequiredArgsConstructor;
import mr.popo.localaiagent.common.exception.BusinessException;
import mr.popo.localaiagent.common.exception.ResourceNotFoundException;
import mr.popo.localaiagent.document.domain.KnowledgeBase;
import mr.popo.localaiagent.document.dto.CreateKbRequest;
import mr.popo.localaiagent.document.dto.KnowledgeBaseDto;
import mr.popo.localaiagent.document.mapper.DocumentMapper;
import mr.popo.localaiagent.document.repository.KnowledgeBaseRepository;
import mr.popo.localaiagent.science.api.Domain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository repository;
    private final DocumentMapper mapper;

    @Transactional(readOnly = true)
    public List<KnowledgeBaseDto> list(Long ownerId) {
        return repository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeBaseDto get(Long id, Long ownerId) {
        return mapper.toDto(loadOwned(id, ownerId));
    }

    @Transactional
    public KnowledgeBaseDto create(Long ownerId, CreateKbRequest request) {
        String slug = slugify(request.name());
        if (repository.existsByOwnerIdAndSlug(ownerId, slug)) {
            throw new BusinessException("A knowledge base with this name already exists");
        }
        KnowledgeBase kb = new KnowledgeBase();
        kb.setOwnerId(ownerId);
        kb.setName(request.name());
        kb.setSlug(slug);
        kb.setDescription(request.description());
        kb.setDomain(request.domain() != null ? request.domain() : Domain.GENERIC);
        return mapper.toDto(repository.save(kb));
    }

    @Transactional
    public void delete(Long id, Long ownerId) {
        repository.delete(loadOwned(id, ownerId));
        // cascade ON DELETE supprime documents + chunks
    }

    @Transactional(readOnly = true)
    public KnowledgeBase loadOwned(Long id, Long ownerId) {
        return repository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> ResourceNotFoundException.of("KnowledgeBase", id));
    }

    static String slugify(String name) {
        String norm = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = norm.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "kb" : slug;
    }
}
