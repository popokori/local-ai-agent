package mr.popo.localaiagent.rag.vector;

public record VectorHit(
        Long chunkId,
        Long documentId,
        Long kbId,
        int ordinal,
        String text,
        Integer pageNumber,
        double score
) {}
