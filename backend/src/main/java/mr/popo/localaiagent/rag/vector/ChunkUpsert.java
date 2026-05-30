package mr.popo.localaiagent.rag.vector;

public record ChunkUpsert(
        int ordinal,
        String text,
        Integer pageNumber,
        Integer tokenCount,
        float[] embedding
) {}
