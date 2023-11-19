package octi.wanparty.http2;

import com.twitter.hpack.Decoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import octi.wanparty.http2.frame.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Http2FrameDecoder extends SimpleChannelInboundHandler<ByteBuf> {

    public static final AttributeKey<Decoder> HPACK_DECODER = AttributeKey.newInstance("hpack_decoder");

    private final static Map<Byte, Class<? extends HttpFrame>> FRAME_TYPES;

    static {
        Map<Byte, Class<? extends HttpFrame>> frameTypes = new HashMap<>();
        frameTypes.put(HttpFrame.SETTINGS, HttpSettingsFrame.class);
        frameTypes.put(HttpFrame.PING, HttpPingFrame.class);
        frameTypes.put(HttpFrame.HEADERS, HttpHeaderFrame.class);
        frameTypes.put(HttpFrame.DATA, HttpDataFrame.class);
        frameTypes.put(HttpFrame.GO_AWAY, HttpGoAwayFrame.class);
        frameTypes.put(HttpFrame.RST_STREAM, HttpResetStreamFrame.class);
        FRAME_TYPES = Collections.unmodifiableMap(frameTypes);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(HPACK_DECODER).set(new Decoder(4096, 4096));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        RawHttpFrame msg = new RawHttpFrame(buf.readByte(), buf.readByte(), buf.readInt() & 0x7FFFFFFF, buf.readBytes(buf.readableBytes()));

        if (!FRAME_TYPES.containsKey(msg.type)) {
            System.err.println("Unknown frame: " + msg);
            return;
        }

        HttpFrame httpFrame = FRAME_TYPES.get(msg.type).newInstance();
        httpFrame.readFromRawFrame(ctx, msg);
        ctx.fireChannelRead(httpFrame);
    }
}
