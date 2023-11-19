package octi.wanparty.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import octi.wanparty.http2.frame.HttpFrame;
import octi.wanparty.http2.frame.HttpGoAwayFrame;
import octi.wanparty.http2.frame.HttpPingFrame;
import octi.wanparty.http2.frame.HttpSettingsFrame;

import java.util.Collections;

public class Http2Handler extends SimpleChannelInboundHandler<HttpFrame> {
    private boolean firstMessage = true;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpFrame msg) throws Exception {
        if (firstMessage) {
            firstMessage = false;
            HttpSettingsFrame response = new HttpSettingsFrame();
            response.settings = Collections.singletonMap(HttpSettingsFrame.Settings.SETTINGS_INITIAL_WINDOW_SIZE, Integer.MAX_VALUE);
            ctx.channel().writeAndFlush(response);
        }
        // System.err.println("Recv: " + msg);
        if (msg instanceof HttpSettingsFrame) {
            HttpSettingsFrame settingsFrame = (HttpSettingsFrame) msg;
            if (settingsFrame.ack)
                return;

            Integer maxHeaderListSize = settingsFrame.settings.get(HttpSettingsFrame.Settings.SETTINGS_MAX_HEADER_LIST_SIZE);
            if (maxHeaderListSize != null)
                ctx.channel().attr(Http2FrameDecoder.HPACK_DECODER).get().setMaxHeaderTableSize(maxHeaderListSize);

            HttpSettingsFrame response = new HttpSettingsFrame();
            response.settings = Collections.emptyMap();
            response.ack = true;
            ctx.channel().writeAndFlush(response);
        } else if (msg instanceof HttpPingFrame) {
            HttpPingFrame pingFrame = (HttpPingFrame) msg;
            pingFrame.ack = true;
            ctx.channel().writeAndFlush(pingFrame);
        } else if (msg instanceof HttpGoAwayFrame) {
            ctx.channel().close();
        }
        ctx.fireChannelRead(msg);
    }
}
