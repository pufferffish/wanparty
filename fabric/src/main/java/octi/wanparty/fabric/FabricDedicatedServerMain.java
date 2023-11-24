package octi.wanparty.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import octi.wanparty.common.WANParty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.SERVER)
public class FabricDedicatedServerMain implements DedicatedServerModInitializer {
    private static final Logger LOGGER = LogManager.getLogger(FabricDedicatedServerMain.class.getSimpleName());

    public boolean hasPostSetupDone = false;

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
            if (this.hasPostSetupDone) {
                return;
            }

            this.hasPostSetupDone = true;
            LOGGER.info("Dedicated server initialized at " + server.getPort());
            WANParty.initServer(server.getPort());
        });
    }

}
