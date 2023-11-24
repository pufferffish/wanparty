package octi.wanparty.http2.frame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import octi.wanparty.http2.RawHttpFrame;
import octi.wanparty.http2.exception.HttpProtocolException;

public final class HttpDataFrame implements HttpFrame {
    public ByteBuf payload;
    public boolean endStream;
    public int streamID;

    @Override
    public void readFromRawFrame(ChannelHandlerContext ctx, RawHttpFrame rawHttpFrame) throws HttpProtocolException {
        rawHttpFrame.assertFrameType(HttpFrame.DATA);
        if ((rawHttpFrame.flags & 0xFE) != 0)
            throw new HttpProtocolException("Unsupported header flags: " + rawHttpFrame.flags);
        this.endStream = (rawHttpFrame.flags & 1) == 1;
        this.streamID = rawHttpFrame.streamID;
        this.payload = rawHttpFrame.payload;
    }

    @Override
    public RawHttpFrame writeToRawFrame(ChannelHandlerContext ctx) {
        return new RawHttpFrame(HttpFrame.DATA, (byte) (this.endStream ? 1 : 0), this.streamID, this.payload);
    }

    @Override
    public String toString() {
        return "HttpDataFrame{" +
                "payload=" + payload +
                ", endStream=" + endStream +
                ", streamID=" + streamID +
                '}';
    }
}
