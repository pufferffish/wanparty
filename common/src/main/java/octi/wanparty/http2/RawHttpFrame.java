package octi.wanparty.http2;

import io.netty.buffer.ByteBuf;
import octi.wanparty.http2.exception.HttpProtocolException;

public class RawHttpFrame {
    public final byte type;
    public final byte flags;
    public final int streamID;
    public final ByteBuf payload;

    public RawHttpFrame(byte type, byte flags, int streamID, ByteBuf payload) {
        this.type = type;
        this.flags = flags;
        this.streamID = streamID;
        this.payload = payload;
    }

    public void assertFrameType(int got) {
        if (this.type != got)
            throw new AssertionError(String.format("Frame type expected: %d, got: %d", this.type, got));
    }

    public void assertStreamID(int got) throws HttpProtocolException {
        if (this.streamID != got)
            throw new HttpProtocolException(String.format("Stream ID should be %d, got: %d", this.streamID, got));
    }

    @Override
    public String toString() {
        return "RawHttpFrame{" +
                "type=" + type +
                ", flags=" + flags +
                ", streamID=" + streamID +
                ", payload=" + payload +
                '}';
    }
}
