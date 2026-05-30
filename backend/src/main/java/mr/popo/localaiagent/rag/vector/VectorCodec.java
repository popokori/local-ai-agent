package mr.popo.localaiagent.rag.vector;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Sérialisation des embeddings float[] ↔ byte[] (PG BYTEA) en
 * float32 little-endian. Format compact (4 octets par dimension).
 */
public final class VectorCodec {

    private VectorCodec() {}

    public static byte[] encode(float[] vector) {
        ByteBuffer buf = ByteBuffer.allocate(vector.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : vector) buf.putFloat(v);
        return buf.array();
    }

    public static float[] decode(byte[] bytes) {
        if (bytes.length % Float.BYTES != 0) {
            throw new IllegalArgumentException("Invalid embedding byte length: " + bytes.length);
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] vector = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < vector.length; i++) vector[i] = buf.getFloat();
        return vector;
    }
}
