package octi.wanparty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import octi.wanparty.cloudflare.Cloudflare;
import octi.wanparty.proxy.ProxyThread;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Arrays;

public class WANPartyClient implements ClientModInitializer {

	public static boolean shareToWan = true;
	public static int proxyPort = 0;
	private static Cloudflared cloudflared = null;

	@Override
	public void onInitializeClient() {
		try {
			ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getByName(null));
			proxyPort = serverSocket.getLocalPort();
			System.out.println("Proxy started on port: " + proxyPort);
			new Thread(new ProxyThread(serverSocket), "WAN Party Proxy").start();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void startProxy(int port) throws IOException {
		if (cloudflared != null) {
			cloudflared.close();
		}

		TunnelAddress tunnel = Cloudflare.createTunnel();
		final String tunnelName = "#" + tunnel.hostname.split("\\.")[0];

		cloudflared = new Cloudflared();
		cloudflared.startProxy(tunnel, port).addListener((ChannelFutureListener) channelFuture -> {
			MinecraftClient client = MinecraftClient.getInstance();
            if (channelFuture.isDone()) {
				if (channelFuture.isSuccess()) {
					Text text = Texts.join(
						Arrays.asList(Text.literal("Other people can join at"), Texts.bracketedCopyable(tunnelName)),
						Text.literal(" ")
					);
					client.inGameHud.getChatHud().addMessage(text);
				} else {
					client.inGameHud.getChatHud().addMessage(Text.of("Failed to share LAN server."));
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

}
