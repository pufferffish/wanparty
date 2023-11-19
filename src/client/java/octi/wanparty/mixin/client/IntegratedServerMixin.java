package octi.wanparty.mixin.client;

import com.mojang.datafixers.DataFixer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import net.minecraft.util.ApiServices;
import net.minecraft.world.GameMode;
import net.minecraft.world.level.storage.LevelStorage;
import octi.wanparty.WANPartyClient;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.net.Proxy;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin extends MinecraftServer {

    public IntegratedServerMixin(Thread serverThread, LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, Proxy proxy, DataFixer dataFixer, ApiServices apiServices, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory) {
        super(serverThread, session, dataPackManager, saveLoader, proxy, dataFixer, apiServices, worldGenerationProgressListenerFactory);
    }

    @Inject(at = @At("RETURN"), method = "openToLan(Lnet/minecraft/world/GameMode;ZI)Z")
    private void hook$openToLan(@Nullable GameMode gameMode, boolean cheatsAllowed, int port, CallbackInfoReturnable<Boolean> ci) {
        try {
            WANPartyClient.startProxy(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Inject(at = @At("RETURN"), method = "setupServer()Z")
    private void hook$setupServer(CallbackInfoReturnable<Boolean> ci) {
        this.setOnlineMode(false);
    }

    @Inject(at = @At("RETURN"), method = "stop(Z)V")
    private void hook$stop(boolean bl, CallbackInfo ci) {
        WANPartyClient.stopProxy();
    }

    @Inject(at = @At("RETURN"), method = "shutdown()V")
    private void hook$shutdown(CallbackInfo ci) {
        WANPartyClient.stopProxy();
    }

}
