package octi.wanparty.http2.frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import octi.wanparty.http2.RawHttpFrame;
import octi.wanparty.http2.exception.HttpProtocolException;

public final class HttpPingFrame implements HttpFrame {
    public boolean ack;
    public long data;

    @Override
    public void readFromRawFrame(ChannelHandlerContext ctx, RawHttpFrame rawHttpFrame) throws HttpProtocolException {
        rawHttpFrame.assertFrameType(HttpFrame.PING);
        rawHttpFrame.assertStreamID(0);
        if (rawHttpFrame.payload.readableBytes() != 8) {
            throw new HttpProtocolException("Ping payload should be 8 bytes");
        }
        ack = (rawHttpFrame.flags & 1) == 1;
        data = rawHttpFrame.payload.readLong();
    }

    @Override
    public RawHttpFrame writeToRawFrame(ChannelHandlerContext ctx) {
        ByteBuf payload = Unpooled.buffer(8);
        payload.writeLong(this.data);
        return new RawHttpFrame(HttpFrame.PING, this.ack ? (byte) 1 : (byte) 0, 0, payload);
    }

    @Override
    public String toString() {
        return "HttpPingFrame{" +
                "ack=" + ack +
                ", data=" + data +
                '}';
    }
}
