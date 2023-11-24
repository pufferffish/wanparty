package octi.wanparty.common;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import octi.wanparty.Cloudflared;
import octi.wanparty.TunnelAddress;
import octi.wanparty.cloudflare.Cloudflare;
import octi.wanparty.proxy.ProxyThread;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

public enum WANParty {
    ;

    public static final String MOD_ID = "wanparty";
    public static final String MOD_NAME = "WAN Party";

    private static final Logger LOGGER = WPLogger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    public static boolean shareToWan = true;
    public static int proxyPort = 0;
    private static Cloudflared cloudflared = null;

    public static void initClient() {
        logVersion();
        try {
            ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getByName(null));
            proxyPort = serverSocket.getLocalPort();
            LOGGER.info("Proxy started on port: " + proxyPort);
            new Thread(new ProxyThread(serverSocket), "WAN Party Proxy").start();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void startProxy(int port, Consumer<String> callback) throws IOException {
        if (cloudflared != null) {
            cloudflared.close();
        }

        TunnelAddress tunnel = Cloudflare.createTunnel();
        final String tunnelName = "#" + tunnel.hostname.split("\\.")[0];

        cloudflared = new Cloudflared();
        cloudflared.startProxy(tunnel, port).addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isDone()) {
                if (channelFuture.isSuccess()) {
                    callback.accept(tunnelName);
                } else {
                    callback.accept(null);
                }
            }
        });
    }

    public static void stopProxy() {
        if (cloudflared != null) {
            cloudflared.close();
            cloudflared = null;
        }
    }

    public static void initServer(int port) {
        logVersion();
        try {
            TunnelAddress tunnel = Cloudflare.createTunnel();
            final String tunnelName = "#" + tunnel.hostname.split("\\.")[0];
            Cloudflared cloudflared = new Cloudflared();
            ChannelFuture future = cloudflared.startProxy(tunnel, port).sync();
            if (future.isSuccess())
                LOGGER.info("Other people can join at {}", tunnelName);
            else
                LOGGER.error("Failed to share LAN server.");
        } catch (Exception e) {
            LOGGER.error("Failed to share LAN server.");
            e.printStackTrace();
        }
        LOGGER.info("Starting server on port " + port);
    }

    private static void logVersion() {
        LOGGER.info("WANParty Branch: " + ModJarInfo.Git_Branch);
        LOGGER.info("WANParty Commit: " + ModJarInfo.Git_Commit);
        LOGGER.info("WANParty Jar Build Source: " + ModJarInfo.Build_Source);
    }

}
