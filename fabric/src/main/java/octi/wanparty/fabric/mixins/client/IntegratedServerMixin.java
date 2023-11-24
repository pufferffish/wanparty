package octi.wanparty.fabric.mixins.client;

import com.mojang.datafixers.DataFixer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.GameType;

import octi.wanparty.common.WANParty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.net.Proxy;
import java.util.Arrays;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin extends MinecraftServer {
    public IntegratedServerMixin(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory) {
        super(thread, levelStorageAccess, packRepository, worldStem, proxy, dataFixer, services, chunkProgressListenerFactory);
    }

    @Inject(at = @At("RETURN"), method = "publishServer(Lnet/minecraft/world/level/GameType;ZI)Z")
    private void hook$publishServer(GameType gameMode, boolean cheats, int port, CallbackInfoReturnable<Boolean> ci) {
        try {
            WANParty.startProxy(port, tunnel -> {
                Minecraft mc = Minecraft.getInstance();
                if (tunnel == null) {
                    mc.gui.getChat().addMessage(Component.literal("Failed to share LAN server."));
                } else {
                    Component text = ComponentUtils.formatList(
                        Arrays.asList(Component.literal("Other people can join at"), ComponentUtils.copyOnClickText(tunnel)),
                        Component.literal(" ")
                    );
                    mc.gui.getChat().addMessage(text);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Inject(at = @At("RETURN"), method = "initServer()Z")
    private void hook$initServer(CallbackInfoReturnable<Boolean> ci) {
        this.setUsesAuthentication(false);
    }

    @Inject(at = @At("RETURN"), method = "halt(Z)V")
    private void hook$halt(boolean bl, CallbackInfo ci) {
        WANParty.stopProxy();
    }

    @Inject(at = @At("RETURN"), method = "stopServer()V")
    private void hook$stopServer(CallbackInfo ci) {
        WANParty.stopProxy();
    }

}
