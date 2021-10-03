package eu.midnightdust.picturesign;

import eu.midnightdust.picturesign.config.PictureSignConfig;
import net.fabricmc.api.ClientModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PictureSignClient implements ClientModInitializer {
    public static Logger LOGGER = LogManager.getLogger("PictureSign");
    @Override
    public void onInitializeClient() {
        PictureSignConfig.init("picturesign", PictureSignConfig.class);
    }
}
