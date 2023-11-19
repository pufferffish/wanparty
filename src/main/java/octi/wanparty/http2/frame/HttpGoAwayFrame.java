package octi.wanparty.http2.frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import octi.wanparty.http2.RawHttpFrame;
import octi.wanparty.http2.exception.HttpProtocolException;

import java.util.Arrays;

public final class HttpGoAwayFrame implements HttpFrame {

    public int lastStreamID;
    public int errorCode;
    public byte[] debugData;

    @Override
    public void readFromRawFrame(ChannelHandlerContext ctx, RawHttpFrame rawHttpFrame) throws HttpProtocolException {
        rawHttpFrame.assertFrameType(HttpFrame.GO_AWAY);
        if (rawHttpFrame.streamID != 0)
            throw new HttpProtocolException("Go Away Frame should have stream ID 0");
        ByteBuf payload = rawHttpFrame.payload;
        lastStreamID = payload.readInt();
        errorCode = payload.readInt();
        debugData = new byte[payload.readableBytes()];
        payload.readBytes(debugData);
    }

    @Override
    public RawHttpFrame writeToRawFrame(ChannelHandlerContext ctx) {
        ByteBuf payload = Unpooled.buffer(8 + debugData.length);
        payload.writeInt(lastStreamID).writeInt(errorCode).writeBytes(debugData);
        return new RawHttpFrame(HttpFrame.GO_AWAY, (byte) 0, 0, payload);
    }

    @Override
    public String toString() {
        return "HttpGoAwayFrame{" +
                "lastStreamID=" + lastStreamID +
                ", errorCode=" + errorCode +
                ", debugData=" + Arrays.toString(debugData) +
                '}';
    }
}
