package satisfy.farmcharm.fabric;

import net.fabricmc.api.ClientModInitializer;
import satisfy.farmcharm.client.FarmCharmClient;

public class FarmCharmClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FarmCharmClient.preInitClient();
        FarmCharmClient.onInitializeClient();
    }
}
