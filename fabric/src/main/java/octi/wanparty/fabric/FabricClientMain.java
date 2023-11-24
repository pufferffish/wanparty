package octi.wanparty.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import octi.wanparty.common.WANParty;

@Environment(EnvType.CLIENT)
public class FabricClientMain implements ClientModInitializer {

    // This loads the mod before minecraft loads which causes a lot of issues
    @Override
    public void onInitializeClient() {
        WANParty.initClient();
    }

}
