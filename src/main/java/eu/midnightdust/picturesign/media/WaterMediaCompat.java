package eu.midnightdust.picturesign.media;

import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.media.platform.PeerTubePlatform;
import eu.midnightdust.picturesign.mixin.MediaAPIAccessor;
import org.bytedeco.ffmpeg.global.avutil;
import org.watermedia.api.media.MediaAPI;
import org.watermedia.api.media.platform.IPlatform;

public class WaterMediaCompat {
    public static void init() {
        if (getVersion() == 3) {
            //avutil.av_log_set_level(avutil.AV_LOG_QUIET);
            PictureSignClient.LOGGER.info("Initializing WATERMeDIA v3 compat for multimedia support.");
            MediaAPI.registerPlatform(new PeerTubePlatform());
            MediaAPIAccessor.getPlatforms().pop(); // Line 31 of MediaAPI needs to pop this as well TODO: upstream
            for (IPlatform platform : MediaAPIAccessor.getPlatforms())
                System.out.println(platform.name());
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
