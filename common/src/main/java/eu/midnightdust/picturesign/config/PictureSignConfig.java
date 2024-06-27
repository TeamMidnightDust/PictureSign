package eu.midnightdust.picturesign.config;

import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.GameRenderer;

import java.util.List;
import java.util.function.Supplier;

public class PictureSignConfig extends MidnightConfig {
    private static final String general = "1general";
    private static final String advanced = "advanced";

    @Entry(category = general) public static boolean enabled = true;
    @Entry(category = general) public static boolean enableVideoSigns = true;
    @Entry(min = 0, max = 1000, isSlider = true, category = general) public static int audioDistanceMultiplier = 30;
    @Entry(category = general) public static boolean translucency = false;
    @Entry(category = general) public static boolean fullBrightPicture = false;
    @Entry(category = general) public static boolean helperUi = true;
    @Entry(category = general) public static boolean exceedVanillaLineLength = true;
    @Entry(category = advanced) public static boolean debug = false;
    @Entry(min = 1, max = 10, isSlider = true, category = advanced) public static int maxThreads = 4;
    @Entry(min = 0, max = 2048, isSlider = true, category = general) public static int signRenderDistance = 64;
    @Entry(category = advanced) public static boolean safeMode = true;
    @Entry(category = advanced) public static List<String> safeProviders = Lists.newArrayList("https://i.imgur.com/", "https://i.ibb.co/", "https://pictshare.net/", "https://iili.io/", "https://media1.tenor.com/");
    @Entry(category = advanced) public static List<String> safeGifProviders = Lists.newArrayList("https://i.imgur.com/", "https://media1.tenor.com/");
    @Entry(category = advanced) public static List<String> safeMultimediaProviders = Lists.newArrayList("https://youtube.com/", "https://www.youtube.com/", "https://youtu.be/","https://vimeo.com/");
    @Entry(category = advanced) public static List<String> safeJsonProviders = Lists.newArrayList("https://github.com/", "https://gist.github.com/", "https://www.jsonkeeper.com/", "https://api.npoint.io/", "https://api.jsonsilo.com/");
    @Comment(category = general) public static Comment ebeWarning;
    @Entry(category = advanced) public static MissingImageMode missingImageMode = MissingImageMode.BLACK;
    @Entry(category = advanced) public static PictureShader pictureShader = PictureShader.PosColTexLight;

    public enum MissingImageMode {
        BLACK, MISSING_TEXTURE, TRANSPARENT
    }
    public enum PictureShader {
        PosColTexLight(GameRenderer::getPositionColorTexLightmapProgram),
        RenderTypeCutout(GameRenderer::getRenderTypeCutoutProgram),
        PosTex(GameRenderer::getPositionTexProgram),
        PosTexCol(GameRenderer::getPositionTexColorProgram);

        PictureShader(Supplier<ShaderProgram> program) {
            this.program = program;
        }
        public final Supplier<ShaderProgram> program;
    }
}
