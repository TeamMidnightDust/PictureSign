package eu.midnightdust.picturesign.render;

import com.mojang.blaze3d.systems.RenderSystem;
import eu.midnightdust.lib.util.PlatformFunctions;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.util.GIFHandler;
import eu.midnightdust.picturesign.util.IrisCompat;
import eu.midnightdust.picturesign.util.MediaHandler;
import eu.midnightdust.picturesign.util.PictureDownloader;
import eu.midnightdust.picturesign.util.PictureSignType;
import eu.midnightdust.picturesign.util.PictureURLUtils;
import eu.midnightdust.picturesign.util.records.MediaJsonInfo;
import eu.midnightdust.picturesign.util.records.PictureDimensions;
import eu.midnightdust.picturesign.util.records.PictureOffset;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Iterator;

import static eu.midnightdust.picturesign.PictureSignClient.client;
import static eu.midnightdust.picturesign.PictureSignClient.hasWaterMedia;
import static eu.midnightdust.picturesign.PictureSignClient.id;
import static eu.midnightdust.picturesign.util.PictureSignType.GIF;
import static eu.midnightdust.picturesign.util.PictureSignType.PICTURE;
import static net.minecraft.client.texture.TextureManager.MISSING_IDENTIFIER;

public class PictureRenderer {
    private boolean isSafeUrl;
    private boolean isSafeJsonUrl;

    public static final Text ERROR = Text.translatable("picturesign.error");
    public static final Text MISSING_VLC = ERROR.copy().append(Text.translatable("picturesign.error.missingVLC"));
    public static final Text MISSING_WATERMEDIA = ERROR.copy().append(Text.translatable("picturesign.error.missingWatermedia"));
    public static final Text IMAGE_NOT_FOUND = ERROR.copy().append(Text.translatable("picturesign.error.imageNotFound"));
    public static final Text UNKNOWN_FILETYPE = ERROR.copy().append(Text.translatable("picturesign.error.unknownFiletype"));
    public static final Text UNKNOWN_SIGNTYPE = ERROR.copy().append(Text.translatable("picturesign.error.unknownSigntype"));
    public static final Text UNSAFE_JSON_URL = ERROR.copy().append(Text.translatable("picturesign.error.unsafeJsonUrl"));
    public static final Text UNSAFE_URL = ERROR.copy().append(Text.translatable("picturesign.error.unsafeUrl"));

    public static final Identifier RAW_TEXTURE = id("internal_raw_texture");

    public void render(BlockEntity blockEntity, PictureSignType type, String url, PictureDimensions dimensions, PictureOffset offset, boolean front, MatrixStack matrixStack, VertexConsumerProvider vertices, int light, int overlay) {
        Text errorMessage = null;
        MediaJsonInfo info = null;
        if (!url.contains("://") && !url.startsWith("file:") && !url.startsWith("rp:")) {
            url = "https://" + url;
        }
        checkJsonUrlSafety(url);

        if (url.endsWith(".json") || isSafeJsonUrl) {
            if (PictureSignConfig.safeMode && !isSafeJsonUrl) errorMessage = UNSAFE_JSON_URL;

            if (errorMessage == null) {
                info = PictureURLUtils.infoFromJson(url);
                if (info == null) return;
                url = info.url();

                if (!url.contains("://")) {
                    url = "https://" + url;
                }
            }
        }

        if ((type == PICTURE && !url.contains(".png") && !url.contains(".jpg") && !url.contains(".jpeg") && !url.startsWith("rp:"))
                || (type == GIF && !url.contains(".gif"))) errorMessage = UNKNOWN_FILETYPE;
        if (PictureSignConfig.safeMode && !url.startsWith("file:") && !url.startsWith("rp:") && !isUrlSafe(type, url)) errorMessage = UNSAFE_URL;
        if (!PictureSignConfig.enableMultimediaSigns && type != PICTURE && type != GIF) return;
        if (!MediaHandler.hasValidImplementation() && type != PICTURE) errorMessage = MISSING_WATERMEDIA;

        if (url.startsWith("https://www.youtube.com/watch?v=")) {
            url = url.replace("https://www.", "https://");
        }
        BlockPos pos = blockEntity.getPos();
        String videoSuffix = front ? "_f" : "_b";
        Identifier videoId = id(pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + videoSuffix);

        MediaHandler mediaHandler = null;
        GIFHandler gifHandler = null;
        if (errorMessage == null && MediaHandler.hasValidImplementation()) {
            if (type.isVideo || type.isAudio) mediaHandler = MediaHandler.getOrCreate(videoId, pos);
            else if (type == GIF && hasWaterMedia) gifHandler = GIFHandler.getOrCreate(videoId);
            else {
                MediaHandler.closePlayer(videoId);
                GIFHandler.closePlayer(videoId);
            }
        }

        if (isDisabledViaRedstone(blockEntity.getWorld(), pos)) {
            if (mediaHandler != null && mediaHandler.isWorking() && !mediaHandler.isStopped()) mediaHandler.stop();

            //PictureURLUtils.cachedJsonData.remove(url);
            return;
        }
        else if (mediaHandler != null && mediaHandler.isDeactivated) {
            if (mediaHandler.isWorking() && mediaHandler.isStopped())
                mediaHandler.restart();
        }
        PictureDownloader.PictureData data = null;
        if (errorMessage == null && type == PICTURE) {
            data = PictureDownloader.getInstance().getPicture(url);
            if (data == null || data.identifier == null) errorMessage = IMAGE_NOT_FOUND;
        }
        else if (mediaHandler != null) {
            if (!mediaHandler.isReady()) errorMessage = MISSING_VLC;
            else {
                if (!mediaHandler.playbackStarted && !mediaHandler.hasMedia()) {
                    mediaHandler.play(url, type.isVideo);
                    if (info != null && info.start() > 0) mediaHandler.setTime(info.start());
                    if (type.isLooped && !mediaHandler.hasMedia() && !mediaHandler.playbackStarted)
                        mediaHandler.setRepeat(true);
                }

                if (info != null && info.volume() >= 0) mediaHandler.setMaxVolume(info.volume());
                mediaHandler.setVolumeBasedOnDistance();
                if (info != null && info.start() > 0 && mediaHandler.getTime() < info.start())
                    mediaHandler.setTime(info.start());
                if (info != null && info.end() > 0 && mediaHandler.getTime() > info.end()) {
                    if (type.isLooped) mediaHandler.setTime(info.start() > 0 ? info.start() : 0);
                    else mediaHandler.stop();
                }
            }
        }
        else if (type == GIF && gifHandler != null) {
            if (!gifHandler.hasMedia() && !gifHandler.playbackStarted) {
                gifHandler.play(url);
            }
        }
        else if (errorMessage == null) errorMessage = UNKNOWN_SIGNTYPE;

        if (type.isAudio || offset == null) return;

        Tessellator tessellator = Tessellator.getInstance();

        int l = PictureSignConfig.fullBrightPicture ? 15728880 : light;
        if (PlatformFunctions.isModLoaded("iris") && IrisCompat.isShaderPackInUse())
            RenderSystem.setShader(PictureSignConfig.pictureShader.program);
        else RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapProgram);

        Identifier texture = getMissingTexture();
        if (errorMessage == null) {
            if (type == PICTURE) {
                assert data != null;
                texture = data.identifier;
            } else if (type.isVideo && mediaHandler != null) {
                if (mediaHandler.isWorking()) {
                    int rawTexture = mediaHandler.getTexture();
                    if (rawTexture != -1) {
                        RenderSystem.setShaderTexture(0, rawTexture);
                        texture = RAW_TEXTURE;
                    }
                }
            } else if (gifHandler != null) {
                if (gifHandler.isWorking()) {
                    RenderSystem.setShaderTexture(0, gifHandler.getTexture());
                    texture = RAW_TEXTURE;
                }
            }
        }

        if (texture == null) return;
        if (texture != RAW_TEXTURE) RenderSystem.setShaderTexture(0, texture);

        if (PictureSignConfig.translucency) RenderSystem.enableBlend();
        else RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        matrixStack.push();
        matrixStack.translate(offset.xOffset() + dimensions.x(), dimensions.y(), offset.zOffset() + dimensions.z());
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(offset.yRotation() + dimensions.yRot()));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(dimensions.xRot()));
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(dimensions.zRot()));

        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);

        buffer.vertex(matrix4f, dimensions.width(), 0.0F, 1.0F).color(255, 255, 255, 255).texture(1.0F, 1.0F).light(l).overlay(overlay).next();

        buffer.vertex(matrix4f, dimensions.width(), dimensions.height(), 1.0F).color(255, 255, 255, 255).texture(1.0F, 0.0F).light(l).overlay(overlay).next();

        buffer.vertex(matrix4f, 0.0F, dimensions.height(), 1.0F).color(255, 255, 255, 255).texture(0.0F, 0.0F).light(l).overlay(overlay).next();

        buffer.vertex(matrix4f, 0.0F, 0.0F, 1.0F).color(255, 255, 255, 255).texture(0.0F, 1.0F).light(l).overlay(overlay).next();

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        if (errorMessage != null) renderErrorMessage(errorMessage, client.textRenderer, matrixStack, vertices, dimensions.width(), dimensions.height());
        matrixStack.pop();
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
    }
    private static void renderErrorMessage(Text error, @NotNull TextRenderer textRenderer, @NotNull MatrixStack matrices, VertexConsumerProvider vertices, float width, float height) {
        float scale = Math.min(width, height) / 100;
        matrices.translate(0, height, 1.005f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));
        matrices.scale(scale,scale,scale);
        int wrappedY = 0;
        for(Iterator<OrderedText> textIterator = textRenderer.wrapLines(error, MathHelper.floor(width/scale)).iterator(); textIterator.hasNext(); wrappedY += 9) {
            renderTextWithShadow(textIterator.next(), wrappedY, textRenderer, matrices.peek().getPositionMatrix(), vertices);
        }
    }
    private static void renderTextWithShadow(OrderedText text, int wrappedY, @NotNull TextRenderer textRenderer, Matrix4f matrix, VertexConsumerProvider vertices) {
        textRenderer.draw(text, 0, wrappedY, 0xFFFFFF, false,  matrix, vertices, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, 15728880);
        matrix.translate(0, 0, 0.025f);
        textRenderer.draw(text, 1, wrappedY + 1, 0x555555, false,  matrix, vertices, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, 15728880);
        matrix.translate(0, 0, -0.025f);
    }
    private static final Identifier BLACK_TEXTURE = id("textures/black.png");
    public @Nullable Identifier getMissingTexture() {
        return switch (PictureSignConfig.missingImageMode) {
            case BLACK -> BLACK_TEXTURE;
            case MISSING_TEXTURE -> MISSING_IDENTIFIER;
            default -> null;
        };
    }
    public void checkJsonUrlSafety(String jsonUrl) {
        isSafeJsonUrl = false;
        PictureSignConfig.safeJsonProviders.forEach(safe -> {
            if (!isSafeJsonUrl) isSafeJsonUrl = jsonUrl.startsWith(safe);
        });
    }
    public boolean isUrlSafe(PictureSignType type, String url) {
        isSafeUrl = false;
        if (type == PICTURE) {
            PictureSignConfig.safeProviders.forEach(safe -> {
                if (!isSafeUrl) isSafeUrl = url.startsWith(safe);
            });
        }
        else if (type == GIF) {
            PictureSignConfig.safeGifProviders.forEach(safe -> {
                if (!isSafeUrl) isSafeUrl = url.startsWith(safe);
            });
        }
        else if (type.isVideo || type.isAudio) {
            PictureSignConfig.safeMultimediaProviders.forEach(safe -> {
                if (!isSafeUrl) isSafeUrl = url.startsWith(safe);
            });
        }
        return isSafeUrl;
    }
    public boolean isDisabledViaRedstone(World world, BlockPos pos) {
        if (world == null) return false;

        BlockState state = world.getBlockState(pos.down()).contains(Properties.LIT) ? world.getBlockState(pos.down()) : world.getBlockState(pos.up());
        if (state.contains(Properties.LIT)) return state.get(Properties.LIT).equals(false);

        return false;
    }
}
