package octi.wanparty.http2.frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import octi.wanparty.http2.RawHttpFrame;
import octi.wanparty.http2.exception.HttpProtocolException;

public class HttpWindowUpdateFrame implements HttpFrame {

    public int streamID;
    public int windowSizeIncrement;

    public HttpWindowUpdateFrame(int streamID, int windowSizeIncrement) {
        this.streamID = streamID;
        this.windowSizeIncrement = windowSizeIncrement;
    }

    public HttpWindowUpdateFrame() {
    }

    @Override
    public void readFromRawFrame(ChannelHandlerContext ctx, RawHttpFrame rawHttpFrame) throws HttpProtocolException {
        rawHttpFrame.assertFrameType(HttpFrame.WINDOW_UPDATE);
        if (rawHttpFrame.payload.readableBytes() != 4)
            throw new HttpProtocolException("Reset Stream payload must be 4 bytes");
        this.streamID = rawHttpFrame.streamID;
        this.windowSizeIncrement = rawHttpFrame.payload.readInt();
        if (this.windowSizeIncrement <= 0)
            throw new HttpProtocolException("Window Size Increment must be positive");
    }

    @Override
    public RawHttpFrame writeToRawFrame(ChannelHandlerContext ctx) {
        ByteBuf payload = Unpooled.buffer(4);
        payload.writeInt(this.windowSizeIncrement);
        return new RawHttpFrame(HttpFrame.WINDOW_UPDATE, (byte) 0, this.streamID, payload);
    }
}
