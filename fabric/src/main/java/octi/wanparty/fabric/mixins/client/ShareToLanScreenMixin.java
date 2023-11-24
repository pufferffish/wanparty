package octi.wanparty.fabric.mixins.client;

import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.network.chat.Component;
import octi.wanparty.common.WANParty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShareToLanScreen.class)
public class ShareToLanScreenMixin extends Screen {

    private static final Component SHARE_TO_WAN_TEXT = Component.literal("Share to WAN");
    protected ShareToLanScreenMixin(Component component) {
        super(component);
    }

    @Inject(at = @At("RETURN"), method = "init()V")
    private void hook$init(CallbackInfo ci) {
        WANParty.shareToWan = true;
        this.addRenderableWidget(CycleButton.onOffBuilder(true)
            .create(this.width / 2 - 75, 220, 150, 20, SHARE_TO_WAN_TEXT, (button, shareToWan) -> WANParty.shareToWan = shareToWan));
    }

}
