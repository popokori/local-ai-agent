package mr.popo.localaiagent.rag.dto;

public record RagHit(
        Long chunkId,
        Long documentId,
        String documentName,
        Integer pageNumber,
        String text,
        double score
) {}
