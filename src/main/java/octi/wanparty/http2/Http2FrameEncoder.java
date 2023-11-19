package octi.wanparty.http2;

import com.twitter.hpack.Encoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.AttributeKey;
import octi.wanparty.http2.frame.HttpFrame;

public class Http2FrameEncoder extends MessageToByteEncoder<HttpFrame> {

    public static final AttributeKey<Encoder> HPACK_ENCODER = AttributeKey.newInstance("hpack_encoder");

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(HPACK_ENCODER).set(new Encoder(4096));
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, HttpFrame msg, ByteBuf out) {
        try {
            RawHttpFrame frame = msg.writeToRawFrame(ctx);
            out.writeByte(frame.type);
            out.writeByte(frame.flags);
            out.writeInt(frame.streamID);
            out.writeBytes(frame.payload);
            // System.err.println("Sent: " + msg);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
}
