package octi.wanparty.mixin.client;

import net.minecraft.client.gui.screen.OpenToLanScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import octi.wanparty.WANPartyClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OpenToLanScreen.class)
public class OpenToLanScreenMixin extends Screen {

	private static final Text SHARE_TO_WAN_TEXT = Text.of("Share to WAN");

	protected OpenToLanScreenMixin(Text title) {
		super(title);
	}

	@Inject(at = @At("RETURN"), method = "init()V")
	private void hook$init(CallbackInfo ci) {
		WANPartyClient.shareToWan = true;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(true)
			.build(this.width / 2 - 75, 220, 150, 20, SHARE_TO_WAN_TEXT, (button, shareToWan) -> WANPartyClient.shareToWan = shareToWan));
	}
}
