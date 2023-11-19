package octi.wanparty.mixin.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginNetworkHandlerMixin {

    @Shadow
    GameProfile profile;

    @Shadow
    MinecraftServer server;

    @Inject(
        at = @At("RETURN"),
        method = "onHello(Lnet/minecraft/network/packet/c2s/login/LoginHelloC2SPacket;)V"
    )
    private void hook$onHello(LoginHelloC2SPacket packet, CallbackInfo ci) {
        packet.profileId().map(uuid -> {
            GameProfile profile = new GameProfile(uuid, this.profile.getName());
            this.server.getSessionService().fillProfileProperties(profile, false);
            this.profile = profile;
            return null;
        });
    }

}
