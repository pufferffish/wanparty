package octi.wanparty.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import octi.wanparty.WANPartyClient;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {

    @Inject(at = @At("RETURN"), method = "openToLan(Lnet/minecraft/world/GameMode;ZI)Z")
    private void hook$openToLan(@Nullable GameMode gameMode, boolean cheatsAllowed, int port, CallbackInfoReturnable<Boolean> ci) {
        try {
            WANPartyClient.startProxy(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
