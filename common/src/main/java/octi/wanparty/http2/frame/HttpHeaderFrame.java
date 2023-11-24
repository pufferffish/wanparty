package octi.wanparty.http2.frame;

import com.twitter.hpack.Decoder;
import com.twitter.hpack.Encoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import octi.wanparty.http2.Http2FrameDecoder;
import octi.wanparty.http2.Http2FrameEncoder;
import octi.wanparty.http2.RawHttpFrame;
import octi.wanparty.http2.exception.HttpProtocolException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class HttpHeaderFrame implements HttpFrame {
    public Map<String, String> headers;
    public boolean endStream;
    public int streamID;

    @Override
    public void readFromRawFrame(ChannelHandlerContext ctx, RawHttpFrame rawHttpFrame) throws HttpProtocolException {
        rawHttpFrame.assertFrameType(HttpFrame.HEADERS);
        if ((rawHttpFrame.flags & 0xFE) != 4)
            throw new HttpProtocolException("Unsupported header flags: " + rawHttpFrame.flags);
        Decoder decoder = ctx.channel().attr(Http2FrameDecoder.HPACK_DECODER).get();
        Map<String, String> headers = new HashMap<>();
        synchronized (decoder) {
            try {
                decoder.decode(new ByteBufInputStream(rawHttpFrame.payload), (name, value, sensitive) -> {
                    headers.put(new String(name), new String(value));
                });
                decoder.endHeaderBlock();
            } catch (IOException e) {
                throw new HttpProtocolException(e);
            }
        }
        this.headers = Collections.unmodifiableMap(headers);
        this.endStream = (rawHttpFrame.flags & 1) == 1;
        this.streamID = rawHttpFrame.streamID;
    }

    @Override
    public RawHttpFrame writeToRawFrame(ChannelHandlerContext ctx) {
        Encoder encoder = ctx.channel().attr(Http2FrameEncoder.HPACK_ENCODER).get();
        ByteBuf byteBuf = Unpooled.buffer();
        synchronized (encoder) {
            try (ByteBufOutputStream bbos = new ByteBufOutputStream(byteBuf)) {
                for (Map.Entry<String, String> fields : headers.entrySet()) {
                    encoder.encodeHeader(bbos, fields.getKey().getBytes(), fields.getValue().getBytes(), false);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        byte flags = (byte) (this.endStream ? 5 : 4);
        return new RawHttpFrame(HttpFrame.HEADERS, flags, streamID, byteBuf);
    }

    @Override
    public String toString() {
        return "HttpHeaderFrame{" +
                "headers=" + headers +
                ", endStream=" + endStream +
                ", streamID=" + streamID +
                '}';
    }
}
