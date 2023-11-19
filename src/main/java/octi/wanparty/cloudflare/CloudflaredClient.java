package octi.wanparty.cloudflare;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import octi.wanparty.TunnelAddress;
import octi.wanparty.http2.frame.*;
import octi.wanparty.tunnelrpc.TunnelRPC;
import org.capnproto.MessageBuilder;
import org.capnproto.Text;
import org.capnproto.TextList;
import org.capnproto.TwoPartyClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class CloudflaredClient extends SimpleChannelInboundHandler<HttpFrame> implements AsynchronousByteChannel {

    private static final String INVALID_REQUEST_RESPONSE = "<html><body><center><h1>400 Bad Request</h1></center><hr></body></html>";
    public final CompletableFuture<String> boundRegionName = new CompletableFuture<>();
    private final TunnelAddress registry;
    private final byte[] clientID;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Queue<ReadRequest> controlStreamConsumeQueue = new LinkedList<>();
    private int controlStreamID = -1;
    private ChannelHandlerContext ctx;
    private ByteBuf controlStreamUnread = Unpooled.EMPTY_BUFFER;
    private final int proxyPort;
    private final Int2ObjectMap<Socket> proxyMap;

    public CloudflaredClient(TunnelAddress registry, int proxyPort) {
        this.proxyPort = proxyPort;
        this.proxyMap = new Int2ObjectOpenHashMap<>();
        this.registry = registry;
        UUID clientID = UUID.randomUUID();
        this.clientID = ByteBuffer.wrap(new byte[16]).putLong(clientID.getMostSignificantBits()).putLong(clientID.getLeastSignificantBits()).array();
    }

    private static <T> CompletableFuture<T> failedFuture(Throwable ex) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        this.executor.shutdownNow();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpFrame msg) throws Exception {
        if (msg instanceof HttpHeaderFrame) {
            HttpHeaderFrame headerFrame = (HttpHeaderFrame) msg;
            final String connectionType = headerFrame.headers.get("cf-cloudflared-proxy-connection-upgrade");
            if ("control-stream".equals(connectionType)) {
                this.controlStreamID = headerFrame.streamID;
                HttpHeaderFrame response = new HttpHeaderFrame();
                response.headers = Collections.singletonMap(":status", "200");
                response.endStream = false;
                response.streamID = headerFrame.streamID;
                ctx.channel().writeAndFlush(response);
                this.setupControlStream();
            } else if ("websocket".equals(connectionType) && !headerFrame.endStream) {
                System.out.println("Accepting connection");
                final String websocketKey = headerFrame.headers.get("sec-websocket-key");
                if (websocketKey == null) {
                    this.respondInvalidRequest(ctx, headerFrame.streamID);
                    return;
                }
                Socket socket = new Socket();
                socket.setTcpNoDelay(true);
                socket.setReceiveBufferSize(65536);
                socket.connect(new InetSocketAddress("127.0.0.1", this.proxyPort));
                this.proxyMap.put(headerFrame.streamID, socket);
                new Thread(() -> {
                    try {
                        while (!socket.isClosed()) {
                            byte[] buffer = new byte[2048];
                            int read = socket.getInputStream().read(buffer);
                            if (read < 0)
                                break;
                            if (read == 0)
                                continue;
                            HttpDataFrame dataFrame = new HttpDataFrame();
                            dataFrame.endStream = false;
                            dataFrame.streamID = headerFrame.streamID;
                            dataFrame.payload = Unpooled.wrappedBuffer(buffer, 0, read);
                            ctx.channel().writeAndFlush(dataFrame);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }, "Proxy conncetion").start();

                Map<String, String> headers = new LinkedHashMap<>();
                headers.put("Connection", "Upgrade");
                headers.put("Upgrade", "websocket");
                headers.put("Sec-WebSocket-Version", "13");
                headers.put("Sec-WebSocket-Accept", Cloudflare.calculateAcceptKey(websocketKey));
                final String serializedHeaders = Cloudflare.serializeHeaders(headers);
                headers.clear();
                headers.put(":status", "200"); // 101 is not used because HTTP/2
                headers.put("cf-cloudflared-response-headers", serializedHeaders);
                headers.put("cf-cloudflared-response-meta", "{\"src\": \"origin\"}");
                HttpHeaderFrame response = new HttpHeaderFrame();
                response.headers = Collections.unmodifiableMap(headers);
                response.endStream = false;
                response.streamID = headerFrame.streamID;
                ctx.channel().writeAndFlush(response);
            } else {
                this.respondInvalidRequest(ctx, headerFrame.streamID);
            }
        } else if (msg instanceof HttpDataFrame) {
            HttpDataFrame dataFrame = (HttpDataFrame) msg;
            if (dataFrame.streamID == this.controlStreamID) {
                this.handleControlStreamData(dataFrame.payload);
                return;
            }
            if (!this.proxyMap.containsKey(dataFrame.streamID)) {
                this.respondInvalidRequest(ctx, dataFrame.streamID);
                return;
            }
            Socket socket = this.proxyMap.get(dataFrame.streamID);
            byte[] payload = new byte[dataFrame.payload.readableBytes()];
            dataFrame.payload.readBytes(payload);
            socket.getOutputStream().write(payload);
            if (dataFrame.endStream) {
                this.proxyMap.remove(dataFrame.streamID).close();
            }
        } else if (msg instanceof HttpResetStreamFrame) {
            HttpResetStreamFrame resetStreamFrame = (HttpResetStreamFrame) msg;
            if (resetStreamFrame.streamID == this.controlStreamID) {
                ctx.channel().close();
            }
        }
    }

    protected void respondInvalidRequest(ChannelHandlerContext ctx, int streamID) {
        HttpHeaderFrame headerResponse = new HttpHeaderFrame();
        headerResponse.endStream = false;
        headerResponse.streamID = streamID;
        headerResponse.headers = Collections.singletonMap(":status", "400");
        ctx.channel().writeAndFlush(headerResponse);
        HttpDataFrame dataResponse = new HttpDataFrame();
        dataResponse.endStream = true;
        dataResponse.payload = Unpooled.copiedBuffer(INVALID_REQUEST_RESPONSE, StandardCharsets.UTF_8);
        dataResponse.streamID = streamID;
        ctx.channel().writeAndFlush(dataResponse);
    }

    protected void setupControlStream() {
        TwoPartyClient rpcClient = new TwoPartyClient(this);
        TunnelRPC.TunnelServer.Client client = new TunnelRPC.TunnelServer.Client(rpcClient.bootstrap());
        TunnelRPC.RegistrationServer.Client.Methods.registerConnection.Request request = client.registerConnectionRequest();
        TunnelRPC.RegistrationServer.RegisterConnectionParams.Builder params = request.getParams();

        TunnelRPC.TunnelAuth.Builder auth = params.getAuth();
        auth.setAccountTag(this.registry.accountTag);
        auth.setTunnelSecret(this.registry.secret);
        params.setTunnelId(this.registry.tunnelID());
        params.setConnIndex((byte) 0);

        TunnelRPC.ConnectionOptions.Builder options = params.getOptions();
        options.setOriginLocalIp(new byte[]{10, 25, 25, 1});
        options.setReplaceExisting(true);
        options.setNumPreviousAttempts((byte) 0);
        options.setCompressionQuality((byte) 0);

        TunnelRPC.ClientInfo.Builder clientInfo = options.getClient();
        clientInfo.setClientId(this.clientID);
        clientInfo.setArch("linux_amd64");
        clientInfo.setVersion("DEV");

        TextList.Builder features = new MessageBuilder().initRoot(TextList.factory);
        features.set(0, new Text.Reader("serialized_headers"));
        clientInfo.setFeatures(features.asReader());

        CompletableFuture
                .supplyAsync(() -> rpcClient.runUntil(request.send()), this.executor)
                .thenCompose(e -> e)
                .thenApply(r -> r.getResult().getResult())
                .thenCompose(r -> {
                    switch (r.which()) {
                        case ERROR:
                            TunnelRPC.ConnectionError.Reader error = r.getError();
                            String cause = error.getCause().toString();
                            if (error.getShouldRetry()) {
                                return failedFuture(new TunnelRegistrationException(cause, error.getRetryAfter()));
                            }
                            return failedFuture(new TunnelRegistrationException(cause, -1));
                        case CONNECTION_DETAILS:
                            return CompletableFuture.completedFuture(r.getConnectionDetails());
                        default:
                            return failedFuture(new TunnelRegistrationException("unknown error", -1));
                    }
                })
                .handle((r, err) -> {
                    if (err != null) {
                        this.close();
                        this.boundRegionName.completeExceptionally(err);
                    } else {
                        this.boundRegionName.complete(r.getLocationName().toString());
                    }
                    this.executor.shutdownNow();
                    return null;
                });
    }

    protected void handleControlStreamData(ByteBuf data) {
        if (data.readableBytes() == 0) {
            return;
        }

        synchronized (this) {
            this.controlStreamUnread = Unpooled.wrappedBuffer(this.controlStreamUnread, data);
            ReadRequest req = this.controlStreamConsumeQueue.peek();
            if (req != null && this.controlStreamUnread.readableBytes() >= req.want) {
                req.read(this.controlStreamUnread);
                this.controlStreamConsumeQueue.poll();
            }
            this.discardUnreadIfEmpty();
        }
    }

    @Override
    public <A> void read(ByteBuffer dst, final A attachment, CompletionHandler<Integer, ? super A> handler) {
        Consumer<ByteBuf> consumer = (buf) -> {
            int read = dst.remaining();
            buf.readBytes(dst);
            handler.completed(read, attachment);
        };
        synchronized (this) {
            if (this.controlStreamUnread.readableBytes() >= dst.remaining()) {
                consumer.accept(this.controlStreamUnread);
            } else {
                this.controlStreamConsumeQueue.add(new ReadRequest(consumer, dst.remaining()));
            }
            this.discardUnreadIfEmpty();
        }
    }

    private void discardUnreadIfEmpty() {
        if (this.controlStreamUnread.readableBytes() == 0) {
            this.controlStreamUnread = Unpooled.EMPTY_BUFFER;
        }
    }

    @Override
    public Future<Integer> read(ByteBuffer dst) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> void write(ByteBuffer src, A attachment, CompletionHandler<Integer, ? super A> handler) {
        try {
            HttpDataFrame dataFrame = new HttpDataFrame();
            dataFrame.endStream = false;
            dataFrame.streamID = this.controlStreamID;
            dataFrame.payload = Unpooled.wrappedBuffer(src);
            ctx.channel().writeAndFlush(dataFrame).addListener((v) -> {
                int written = src.remaining();
                src.position(written);
                handler.completed(written, attachment);
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            handler.failed(ex, attachment);
        }
    }

    @Override
    public Future<Integer> write(ByteBuffer src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen() {
        return this.controlStreamID > 0;
    }

    @Override
    public void close() {
        HttpDataFrame dataFrame = new HttpDataFrame();
        dataFrame.endStream = true;
        dataFrame.streamID = this.controlStreamID;
        dataFrame.payload = Unpooled.EMPTY_BUFFER;
        ctx.channel().writeAndFlush(dataFrame);
        this.controlStreamID = -1;
    }

    private static class ReadRequest {
        public final int want;
        private final Consumer<ByteBuf> reader;

        private ReadRequest(Consumer<ByteBuf> reader, int want) {
            this.reader = reader;
            this.want = want;
        }

        public void read(ByteBuf buf) {
            this.reader.accept(buf);
        }
    }

}
