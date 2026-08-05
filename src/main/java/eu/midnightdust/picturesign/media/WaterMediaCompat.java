package eu.midnightdust.picturesign.media;

import eu.midnightdust.picturesign.PictureSignClient;
import org.watermedia.api.platform.PlatformAPI;

public class WaterMediaCompat {
    public static void init() {
        if (getVersion() == 3) {
            PictureSignClient.LOGGER.info("Initializing WATERMeDIA v3 compat for multimedia support.");
            //PlatformAPI.register(new PeerTubePlatform());
//            MediaAPIAccessor.getPlatforms().pop(); // Line 31 of MediaAPI needs to pop this as well TODO: upstream
//            for (IPlatform platform : MediaAPIAccessor.getPlatforms())
//                System.out.println(platform.name());
            MediaHandler.registerHandler(WM3MediaHandler::new);
            GIFHandler.registerHandler(WM3GIFHandler::new);
        }
    }
    private static int getVersion() {
        try {
            Class.forName("org.watermedia.api.media.MRL", false, WaterMediaCompat.class.getClassLoader());
            return 3;
        } catch (ClassNotFoundException ignored) {}
        try {
            Class.forName("org.watermedia.api.player.PlayerAPI", false, WaterMediaCompat.class.getClassLoader());
            return 2;
        } catch (ClassNotFoundException ignored) {}
        return 0;
    }
}
