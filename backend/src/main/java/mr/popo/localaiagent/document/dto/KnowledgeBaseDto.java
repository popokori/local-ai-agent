package mr.popo.localaiagent.document.dto;

import mr.popo.localaiagent.science.api.Domain;

import java.time.OffsetDateTime;

public record KnowledgeBaseDto(
        Long id,
        String name,
        String slug,
        String description,
        Domain domain,
        String embeddingModel,
        Integer embeddingDim,
        OffsetDateTime createdAt
) {}
