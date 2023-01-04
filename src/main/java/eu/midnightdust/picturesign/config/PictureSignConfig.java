package eu.midnightdust.picturesign.config;

import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;

import java.util.List;

public class PictureSignConfig extends MidnightConfig {
    @Entry public static boolean enabled = true;
    @Entry public static boolean enableVideoSigns = true;
    @Entry public static boolean translucency = false;
    @Entry public static boolean helperUi = true;
    @Entry public static boolean exceedVanillaLineLength = true;
    @Entry public static boolean debug = false;
    @Entry(min = 1, max = 10) public static int maxThreads = 4;
    @Entry(min = 0, max = 4096) public static int signRenderDistance = 64;
    @Entry public static boolean safeMode = true;
    @Comment public static Comment ebeWarning;
    @Comment public static Comment ebeWarning2;
    @Entry public static List<String> safeProviders = Lists.newArrayList("https://i.imgur.com/", "https://i.ibb.co/", "https://pictshare.net/", "https://iili.io/");
    @Entry public static MissingImageMode missingImageMode = MissingImageMode.BLACK;

    public enum MissingImageMode {
        BLACK, MISSING_TEXTURE, TRANSPARENT
    }
}
