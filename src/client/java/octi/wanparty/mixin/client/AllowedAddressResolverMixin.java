package octi.wanparty.mixin.client;

import net.minecraft.client.network.AllowedAddressResolver;
import net.minecraft.client.network.ServerAddress;
import octi.wanparty.WANPartyClient;
import octi.wanparty.util.FakeObjectOutputStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.network.Address;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AllowedAddressResolver.class)
public class AllowedAddressResolverMixin {

    @Inject(method = "resolve(Lnet/minecraft/client/network/ServerAddress;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    public void resolve(ServerAddress address, CallbackInfoReturnable<Optional<Address>> cir) {
        InetSocketAddress proxyAddress;
        if (address.getAddress().startsWith("#")) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new FakeObjectOutputStream(baos, address.getAddress())) {
                oos.writeObject(new InetSocketAddress("127.0.0.1", WANPartyClient.proxyPort));
                oos.flush();
                proxyAddress = (InetSocketAddress) new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            cir.setReturnValue(Optional.of(Address.create(proxyAddress)));
            cir.cancel();
        }
    }


}
