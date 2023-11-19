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

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class CloudflareServerChannel extends AbstractServerChannel {

    private final EventLoopGroup workerGroup;
    private final NioSocketChannel socketChannel;
    private final Bootstrap bootstrap;
    private CloudflaredClient client = null;
    private Channel channel;
    private TunnelAddress registry;

    public CloudflareServerChannel() {
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
                            .addLast(CloudflareServerChannel.this.client);
                    ch.pipeline()
                            .addLast(new LengthFieldPrepender(3, -6))
                            .addLast(new Http2FrameEncoder());
                }
            });
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof SingleThreadEventLoop;
    }

    @Override
    protected SocketAddress localAddress0() {
        return this.registry;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        if (this.client != null) {
            throw new IllegalStateException("Server channel already bound");
        }
        if (!(localAddress instanceof TunnelAddress)) {
            throw new UnsupportedOperationException("Expected TunnelAddress");
        }
        final TunnelAddress registry = (TunnelAddress) localAddress;
        this.client = new CloudflaredClient((TunnelAddress) localAddress);
        InetSocketAddress edge = Cloudflare.resolveEdgeNodes()[0];
        this.channel = this.bootstrap.connect(edge).sync().channel();
        registry.regionName = this.client.boundRegionName.get();
        this.registry = registry;
    }

    @Override
    protected void doClose() throws Exception {
        this.client.close();
        this.channel.close().sync();
        this.workerGroup.shutdownGracefully();
    }

    @Override
    protected void doBeginRead() throws Exception {
        Thread.sleep(10000000);
    }

    @Override
    public ChannelConfig config() {
        return this.socketChannel.config();
    }

    @Override
    public boolean isOpen() {
        if (this.channel == null) {
            return true;
        }
        return this.channel.isOpen();
    }

    @Override
    public boolean isActive() {
        if (this.channel == null) {
            return false;
        }
        return this.channel.isActive();
    }

}
