package octi.wanparty.mixin;

import com.mojang.datafixers.DataFixer;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.util.ApiServices;
import net.minecraft.world.level.storage.LevelStorage;
import octi.wanparty.Cloudflared;
import octi.wanparty.TunnelAddress;
import octi.wanparty.WANParty;
import octi.wanparty.cloudflare.Cloudflare;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.net.Proxy;

@Mixin(MinecraftDedicatedServer.class)
public abstract class MinecraftDedicatedServerMixin extends MinecraftServer {



	public MinecraftDedicatedServerMixin(Thread serverThread, LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, Proxy proxy, DataFixer dataFixer, ApiServices apiServices, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory) {
		super(serverThread, session, dataPackManager, saveLoader, proxy, dataFixer, apiServices, worldGenerationProgressListenerFactory);
	}


	@Inject(at = @At("RETURN"), method = "setupServer()Z")
	private void hook$setupServer(CallbackInfoReturnable<Boolean> ci) {
		if (!ci.getReturnValueZ()) {
			return;
		}

		try {
			TunnelAddress tunnel = Cloudflare.createTunnel();
			final String tunnelName = "#" + tunnel.hostname.split("\\.")[0];

			Cloudflared cloudflared = new Cloudflared();
			ChannelFuture future = cloudflared.startProxy(tunnel, this.getServerPort()).sync();
			if (future.isSuccess())
				WANParty.LOGGER.info("Other people can join at {}", tunnelName);
			else
				WANParty.LOGGER.error("Failed to share LAN server.");
		} catch (Exception ex) {
			ex.printStackTrace();
			ci.setReturnValue(false);
		}
	}

}
