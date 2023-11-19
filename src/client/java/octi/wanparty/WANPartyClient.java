package octi.wanparty;

import net.fabricmc.api.ClientModInitializer;

public class WANPartyClient implements ClientModInitializer {

	public static boolean shareToWan = true;

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
	}
}
