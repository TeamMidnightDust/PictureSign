package eu.midnightdust.picturesign.config;

import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;
import eu.midnightdust.lib.util.PlatformFunctions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import static eu.midnightdust.picturesign.PictureSignClient.id;

public class PictureSignConfig extends MidnightConfig {
    private static final String GENERAL = "general";
    private static final String ADVANCED = "advanced";

    private static final String EBE_MODID = "enhancedblockentities";
    private static final String WM_MODID = "watermedia";
    private static final String IRIS_MODID = "iris";

    @Comment(centered = true, category = GENERAL) public static Comment _features;
    @Entry(category = GENERAL) public static boolean enabled = true;
    @Condition(requiredModId = WM_MODID)
    @Entry(category = GENERAL) public static boolean enableMultimediaSigns = true;
    @Condition(requiredOption = "isWMLoaded", requiredValue = "false")
    @Comment(category = GENERAL, centered = true) public static Comment wmNotice;

    @Comment(centered = true, category = GENERAL) public static Comment _behaviour;
    @Entry(category = GENERAL) public static boolean helperUi = true;
    @Condition(requiredOption = "helperUi", visibleButLocked = true)
    @Entry(category = GENERAL) public static boolean exceedVanillaLineLength = true;
    @Entry(category = GENERAL) public static boolean translucency = false;
    @Entry(category = GENERAL) public static boolean fullBrightPicture = false;
    @Entry(min = 0, max = 2048, isSlider = true, category = GENERAL) public static int signRenderDistance = 64;
    @Condition(requiredModId = WM_MODID)
    @Entry(min = 0, max = 1000, isSlider = true, category = GENERAL) public static int audioDistanceMultiplier = 30;
    @Condition(requiredModId = EBE_MODID)
    @Comment(category = GENERAL) public static Comment ebeWarning;

    @Comment(centered = true, category = ADVANCED) public static Comment _workarounds;
    @Entry(category = ADVANCED) public static MissingImageMode missingImageMode = MissingImageMode.BLACK;
    @Condition(requiredModId = IRIS_MODID)
    @Entry(category = ADVANCED) public static PictureShader pictureShader = PictureShader.PosColTexLight;

    @Comment(centered = true, category = ADVANCED) public static Comment _misc;
    @Entry(category = ADVANCED) public static boolean debug = false;
    @Entry(min = 1, max = 10, isSlider = true, category = ADVANCED) public static int maxThreads = 4;

    @Comment(centered = true, category = ADVANCED) public static Comment _security;
    @Entry(category = ADVANCED) public static boolean safeMode = true;
    @Condition(requiredOption = "safeMode")
    @Entry(category = ADVANCED) public static List<String> safeProviders = Lists.newArrayList("https://i.imgur.com/", "https://i.ibb.co/", "https://pictshare.net/", "https://iili.io/");
    @Condition(requiredModId = WM_MODID)
    @Condition(requiredOption = "safeMode")
    @Entry(category = ADVANCED) public static List<String> safeGifProviders = Lists.newArrayList("https://i.imgur.com/", "https://media1.tenor.com/");
    @Condition(requiredModId = WM_MODID)
    @Condition(requiredOption = "safeMode")
    @Entry(category = ADVANCED) public static List<String> safeMultimediaProviders = Lists.newArrayList("https://youtube.com/", "https://www.youtube.com/", "https://youtu.be/","https://vimeo.com/");
    @Condition(requiredOption = "safeMode")
    @Entry(category = ADVANCED) public static List<String> safeJsonProviders = Lists.newArrayList("https://github.com/", "https://gist.github.com/", "https://www.jsonkeeper.com/", "https://api.npoint.io/", "https://api.jsonsilo.com/");
    @Entry(category = ADVANCED) public static boolean redactApiKeys = true;

    @Condition(requiredModId = "thismodhopefullydoesntexistwejustneedtohidetheentrywhilekeepingitaccessible")
    @Entry(category = GENERAL) public static boolean isWMLoaded = PlatformFunctions.isModLoaded(WM_MODID);

    public enum MissingImageMode {
        BLACK(id("textures/black.png")),
        MISSING_TEXTURE(TextureManager.INTENTIONAL_MISSING_TEXTURE),
        TRANSPARENT(null);

        MissingImageMode(@Nullable ResourceLocation missingTextureId) {
            this.missingTextureId = missingTextureId;
        }
        @Nullable public final ResourceLocation missingTextureId;
    }

    public enum PictureShader {
        PosColTexLight(),
        RenderTypeCutout(),
        PosTex(),
        PosTexCol();

        PictureShader() {
        }
    }

    public static void init(String modid, Class<? extends MidnightConfig> config) {
        MidnightConfig.init(modid, config);
        isWMLoaded = PlatformFunctions.isModLoaded(WM_MODID);
        PictureSignConfig.write(modid);
    }
}
