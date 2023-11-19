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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {

    @Inject(at = @At("RETURN"), method = "openToLan(Lnet/minecraft/world/GameMode;ZI)Z")
    private void hook$openToLan(@Nullable GameMode gameMode, boolean cheatsAllowed, int port, CallbackInfoReturnable<Boolean> ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.inGameHud.getChatHud().addMessage(Text.of("WANParty is " + (WANPartyClient.shareToWan ? "enabled" : "disabled") + "."));
    }

}
