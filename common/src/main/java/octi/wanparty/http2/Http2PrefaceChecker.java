package octi.wanparty.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import octi.wanparty.http2.exception.HttpProtocolException;

import java.util.List;

public class Http2PrefaceChecker extends ByteToMessageDecoder {
    private static final String PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n";

    private boolean checkPass = false;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (this.checkPass) {
            out.add(in.readBytes(in.readableBytes()));
            return;
        }

        if (in.readableBytes() < PREFACE.length()) {
            return;
        }

        ByteBuf magicBytes = in.slice(in.readerIndex(), PREFACE.length());
        String receivedString = magicBytes.toString(CharsetUtil.UTF_8);

        if (PREFACE.equals(receivedString)) {
            in.skipBytes(PREFACE.length());
            this.checkPass = true;
            ctx.fireChannelRead(in.retain());
        } else {
            throw new HttpProtocolException("Expected HTTP/2 preface");
        }
    }
}
