package octi.wanparty.http2.frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import octi.wanparty.http2.RawHttpFrame;
import octi.wanparty.http2.exception.HttpProtocolException;

public final class HttpResetStreamFrame implements HttpFrame {
    public int streamID;
    public int errorCode;

    @Override
    public void readFromRawFrame(ChannelHandlerContext ctx, RawHttpFrame rawHttpFrame) throws HttpProtocolException {
        rawHttpFrame.assertFrameType(HttpFrame.RST_STREAM);
        if (rawHttpFrame.streamID == 0)
            throw new HttpProtocolException("Reset Stream stream ID is 0");
        if (rawHttpFrame.payload.readableBytes() != 4)
            throw new HttpProtocolException("Reset Stream payload must be 4 bytes");
        streamID = rawHttpFrame.streamID;
        errorCode = rawHttpFrame.payload.readInt();
    }

    @Override
    public RawHttpFrame writeToRawFrame(ChannelHandlerContext ctx) {
        ByteBuf byteBuf = Unpooled.buffer(4);
        byteBuf.writeInt(this.errorCode);
        return new RawHttpFrame(HttpFrame.RST_STREAM, (byte) 0, this.streamID, byteBuf);
    }

    @Override
    public String toString() {
        return "HttpResetStreamFrame{" +
                "streamID=" + streamID +
                ", errorCode=" + errorCode +
                '}';
    }
}
