package octi.wanparty.forge.mixins.client;

import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import octi.wanparty.common.WANParty;
import octi.wanparty.util.InetAddressCreator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetSocketAddress;
import java.util.Optional;

@Mixin(ServerNameResolver.class)
public class ServerNameResolverMixin {

    @Inject(method = "resolveAddress(Lnet/minecraft/client/multiplayer/resolver/ServerAddress;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void resolveAddress(ServerAddress address, CallbackInfoReturnable<Optional<ResolvedServerAddress>> cir) {
        if (address.getHost().startsWith("#")) {
            InetSocketAddress proxyAddress = InetAddressCreator.localAddressWithHostname(address.getHost(), WANParty.proxyPort);
            cir.setReturnValue(Optional.of(ResolvedServerAddress.from(proxyAddress)));
            cir.cancel();
        }
    }

}
