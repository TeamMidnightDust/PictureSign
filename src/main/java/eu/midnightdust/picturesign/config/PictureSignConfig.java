package eu.midnightdust.picturesign.config;

import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.GameRenderer;

import java.util.List;
import java.util.function.Supplier;

public class PictureSignConfig extends MidnightConfig {
    public static final String general = "1general";
    private final static String advanced = "advanced";

    @Entry(category = general) public static boolean enabled = true;
    @Entry(category = general) public static boolean enableVideoSigns = true;
    @Entry(category = general) public static boolean translucency = false;
    @Entry(category = general) public static boolean fullBrightPicture = false;
    @Entry(category = general) public static boolean helperUi = true;
    @Entry(category = general) public static boolean exceedVanillaLineLength = true;
    @Entry(category = advanced) public static boolean debug = false;
    @Entry(min = 1, max = 10, isSlider = true, category = advanced) public static int maxThreads = 4;
    @Entry(min = 0, max = 2048, isSlider = true, category = general) public static int signRenderDistance = 64;
    @Entry(category = general) public static boolean safeMode = true;
    @Entry(category = general) public static List<String> safeProviders = Lists.newArrayList("https://i.imgur.com/", "https://i.ibb.co/", "https://pictshare.net/", "https://iili.io/", "https://vimeo.com/", "https://yewtu.be/");
//    @Entry(category = general) public static String invidiousInstance = "yt.oelrichsgarcia.de";
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
        PosColTex(GameRenderer::getPositionColorTexProgram),
        PosTexCol(GameRenderer::getPositionTexColorProgram);

        PictureShader(Supplier<ShaderProgram> program) {
            this.program = program;
        }
        public final Supplier<ShaderProgram> program;
    }
}
