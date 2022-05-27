package eu.midnightdust.picturesign.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class PictureSignConfig extends MidnightConfig {
    @Entry public static boolean enabled = true;
    @Entry public static boolean translucency = false;
    @Entry public static boolean helperUi = true;
    @Entry public static boolean exceedVanillaLineLength = false;
    @Entry public static boolean debug = false;
    @Entry(min = 1, max = 10) public static int maxThreads = 4;
    @Entry(min = 0, max = 4096) public static int signRenderDistance = 64;
    @Entry public static boolean safeMode = true;
    @Comment public static Comment ebeWarning;
    @Comment public static Comment ebeWarning2;
}
