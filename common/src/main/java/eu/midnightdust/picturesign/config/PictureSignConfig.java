package eu.midnightdust.picturesign.config;

import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static eu.midnightdust.picturesign.PictureSignClient.id;

public class PictureSignConfig extends MidnightConfig {
    private static final String GENERAL = "general";
    private static final String ADVANCED = "advanced";

    @Entry(category = GENERAL) public static boolean enabled = true;
    @Entry(category = GENERAL) public static boolean enableMultimediaSigns = true;
    @Entry(min = 0, max = 1000, isSlider = true, category = GENERAL) public static int audioDistanceMultiplier = 30;
    @Entry(category = GENERAL) public static boolean translucency = false;
    @Entry(category = GENERAL) public static boolean fullBrightPicture = false;
    @Entry(category = GENERAL) public static boolean helperUi = true;
    @Entry(category = GENERAL) public static boolean exceedVanillaLineLength = true;
    @Entry(category = ADVANCED) public static boolean debug = false;
    @Entry(min = 1, max = 10, isSlider = true, category = ADVANCED) public static int maxThreads = 4;
    @Entry(min = 0, max = 2048, isSlider = true, category = GENERAL) public static int signRenderDistance = 64;
    @Entry(category = ADVANCED) public static boolean safeMode = true;
    @Entry(category = ADVANCED) public static List<String> safeProviders = Lists.newArrayList("https://i.imgur.com/", "https://i.ibb.co/", "https://pictshare.net/", "https://iili.io/");
    @Entry(category = ADVANCED) public static List<String> safeGifProviders = Lists.newArrayList("https://i.imgur.com/", "https://media1.tenor.com/");
    @Entry(category = ADVANCED) public static List<String> safeMultimediaProviders = Lists.newArrayList("https://youtube.com/", "https://www.youtube.com/", "https://youtu.be/","https://vimeo.com/");
    @Entry(category = ADVANCED) public static List<String> safeJsonProviders = Lists.newArrayList("https://github.com/", "https://gist.github.com/", "https://www.jsonkeeper.com/", "https://api.npoint.io/", "https://api.jsonsilo.com/");
    @Comment(category = GENERAL) @Condition(requiredModId = "enhancedblockentities") public static Comment ebeWarning;
    @Entry(category = ADVANCED) public static MissingImageMode missingImageMode = MissingImageMode.BLACK;
    @Entry(category = ADVANCED) public static PictureShader pictureShader = PictureShader.PosColTexLight;

    public enum MissingImageMode {
        BLACK(id("textures/black.png")),
        MISSING_TEXTURE(TextureManager.MISSING_IDENTIFIER),
        TRANSPARENT(null);

        MissingImageMode(@Nullable Identifier missingTextureId) {
            this.missingTextureId = missingTextureId;
        }
        @Nullable public final Identifier missingTextureId;
    }
    public enum PictureShader {
        PosColTexLight(ShaderProgramKeys.POSITION_COLOR_TEX_LIGHTMAP),
        RenderTypeCutout(ShaderProgramKeys.RENDERTYPE_CUTOUT),
        PosTex(ShaderProgramKeys.POSITION_TEX),
        PosTexCol(ShaderProgramKeys.POSITION_TEX_COLOR);

        PictureShader(ShaderProgramKey program) {
            this.program = program;
        }
        public final ShaderProgramKey program;
    }
}
