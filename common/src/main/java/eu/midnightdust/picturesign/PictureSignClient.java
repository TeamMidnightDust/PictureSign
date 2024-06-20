package eu.midnightdust.picturesign;

import eu.midnightdust.lib.util.PlatformFunctions;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

public class PictureSignClient {
    public static Logger LOGGER = LogManager.getLogger("PictureSign");
    public static final String MOD_ID = "picturesign";
    public static final boolean hasWaterMedia = PlatformFunctions.isModLoaded("watermedia");
    public static String[] clipboard = new String[4];
    public static final MinecraftClient client = MinecraftClient.getInstance();
    public static final KeyBinding BINDING_COPY_SIGN = new KeyBinding("key."+MOD_ID+".copy_sign",
            InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, "key.categories."+MOD_ID);

    public static void init() {
        PictureSignConfig.init(MOD_ID, PictureSignConfig.class);
    }
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
