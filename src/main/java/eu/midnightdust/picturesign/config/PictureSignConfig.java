package eu.midnightdust.picturesign.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class PictureSignConfig extends MidnightConfig {
    @Entry public static boolean enabled = true;
    @Entry public static boolean debug = false;
    @Entry(min = 1, max = 10) public static int maxThreads = 4;
    @Entry(min = 0, max = 1024) public static int signRenderDistance = 64;
}
