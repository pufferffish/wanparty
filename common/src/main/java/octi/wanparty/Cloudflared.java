package octi.wanparty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import octi.wanparty.cloudflare.Cloudflare;
import octi.wanparty.cloudflare.CloudflareCA;
import octi.wanparty.cloudflare.CloudflaredClient;
import octi.wanparty.http2.Http2FrameDecoder;
import octi.wanparty.http2.Http2FrameEncoder;
import octi.wanparty.http2.Http2Handler;
import octi.wanparty.http2.Http2PrefaceChecker;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Cloudflared implements Closeable {

    private CloudflaredClient client;
    private EventLoopGroup workerGroup;
    private NioSocketChannel socketChannel;
    private Bootstrap bootstrap;


    public ChannelFuture startProxy(TunnelAddress tunnelAddress, int proxyPort) throws IOException {
        if (this.client != null) {
            throw new IllegalStateException("Proxy already started");
        }

        this.client = new CloudflaredClient(tunnelAddress, proxyPort);
        this.workerGroup = new NioEventLoopGroup();
        this.socketChannel = new NioSocketChannel();
        this.bootstrap = new Bootstrap()
            .group(workerGroup)
            .channelFactory(() -> this.socketChannel)
            .handler(new ChannelInitializer<Channel>() {
                @Override
                public void initChannel(Channel ch) throws Exception {
                    SslContext sslContext = SslContextBuilder.forClient().trustManager(CloudflareCA.ROOT_CAs).build();
                    ch.pipeline()
                            .addLast(sslContext.newHandler(ch.alloc(), Cloudflare.HTTP2_SNI, 443))
                            .addLast(new Http2PrefaceChecker())
                            .addLast(new LengthFieldBasedFrameDecoder(16384, 0, 3, 6, 3))
                            .addLast(new Http2FrameDecoder())
                            .addLast(new Http2Handler())
                            .addLast(Cloudflared.this.client);
                    ch.pipeline()
                            .addLast(new LengthFieldPrepender(3, -6))
                            .addLast(new Http2FrameEncoder());
                }
            });

        InetSocketAddress edge = Cloudflare.resolveEdgeNodes()[0];
        return this.bootstrap.connect(edge);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.close();
        }
        if (this.workerGroup != null) {
            this.workerGroup.shutdownGracefully();
        }
    }
}
