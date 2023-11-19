package octi.wanparty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import octi.wanparty.cloudflare.Cloudflare;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class WANPartyClient implements ClientModInitializer {

	public static boolean shareToWan = true;
	private static Cloudflared cloudflared = null;

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
	}

	public static void startProxy(int port) throws IOException {
		if (cloudflared != null) {
			cloudflared.close();
		}

		TunnelAddress tunnel = Cloudflare.createTunnel();
		cloudflared = new Cloudflared();
		cloudflared.startProxy(tunnel, port).addListener((ChannelFutureListener) channelFuture -> {
			MinecraftClient client = MinecraftClient.getInstance();
            if (channelFuture.isDone()) {
				if (channelFuture.isSuccess()) {
					client.inGameHud.getChatHud().addMessage(Text.of("Server is listening at " + tunnel.hostname));
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
