package eu.midnightdust.picturesign.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.config.PictureSignSecrets;
import eu.midnightdust.picturesign.media.GIFHandler;
import eu.midnightdust.picturesign.media.MediaHandler;
import eu.midnightdust.picturesign.util.*;
import eu.midnightdust.picturesign.util.records.MediaJsonInfo;
import eu.midnightdust.picturesign.util.records.PictureDimensions;
import eu.midnightdust.picturesign.util.records.PictureOffset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.Optional;

import static eu.midnightdust.picturesign.PictureSignClient.id;
import static eu.midnightdust.picturesign.util.PictureSignType.GIF;
import static eu.midnightdust.picturesign.util.PictureSignType.PICTURE;

//? if < 1.21.4 {
import net.minecraft.client.renderer.GameRenderer;
//?}

public class PictureRenderer {
    private boolean isSafeUrl;
    private boolean isSafeJsonUrl;

    public static final Component ERROR = Component.translatable("picturesign.error");
    public static final Component MISSING_API_KEY = ERROR.copy().append(Component.translatable("picturesign.error.missingApiKey"));
    public static final Component MISSING_VLC = ERROR.copy().append(Component.translatable("picturesign.error.missingNatives", "VLC"));
    public static final Component MISSING_WATERMEDIA = ERROR.copy().append(Component.translatable("picturesign.error.missingWatermedia"));
    public static final Component IMAGE_NOT_FOUND = ERROR.copy().append(Component.translatable("picturesign.error.imageNotFound"));
    public static final Component UNKNOWN_FILETYPE = ERROR.copy().append(Component.translatable("picturesign.error.unknownFiletype"));
    public static final Component UNKNOWN_SIGNTYPE = ERROR.copy().append(Component.translatable("picturesign.error.unknownSigntype"));
    public static final Component UNSAFE_JSON_URL = ERROR.copy().append(Component.translatable("picturesign.error.unsafeJsonUrl"));
    public static final Component UNSAFE_URL = ERROR.copy().append(Component.translatable("picturesign.error.unsafeUrl"));

    public static final ResourceLocation RAW_TEXTURE = id("internal_raw_texture");

    public void render(BlockEntity blockEntity, PictureSignType type, String url, PictureDimensions dimensions, PictureOffset offset, boolean front, PoseStack matrixStack, MultiBufferSource vertices, int light, int overlay) {
        Component errorMessage = null;
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

        Optional<String> resolvedUrl = PictureSignSecrets.resolveSecret(url);
        if (resolvedUrl.isEmpty()) errorMessage = MISSING_API_KEY;
        else url = resolvedUrl.get();

        if (url.startsWith("https://www.youtube.com/watch?v=")) {
            url = url.replace("https://www.", "https://");
        }
        BlockPos pos = blockEntity.getBlockPos();
        String videoSuffix = front ? "_f" : "_b";
        ResourceLocation videoId = id(pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + videoSuffix);

        MediaHandler mediaHandler = null;
        GIFHandler gifHandler = null;
        if (errorMessage == null) {
            if ((type.isVideo || type.isAudio) && MediaHandler.hasValidImplementation()) mediaHandler = MediaHandler.getOrCreate(videoId, pos);
            else if (type == GIF && GIFHandler.hasValidImplementation()) gifHandler = GIFHandler.getOrCreate(videoId);
            else {
                MediaHandler.closePlayer(videoId);
                GIFHandler.closePlayer(videoId);
            }
        }

        if (isDisabledViaRedstone(blockEntity.getLevel(), pos)) {
            if (mediaHandler != null && mediaHandler.isWorking() && !mediaHandler.isStopped()) mediaHandler.stop();
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
            if (!mediaHandler.isWorking()) {
                if (!mediaHandler.hasMedia(url)) mediaHandler.load(url);
                errorMessage = MISSING_VLC;
                if (mediaHandler.isLoaded()) {
                    mediaHandler.play(type.isVideo);
                    if (mediaHandler.isPlaying()) {
                        if (info != null && info.start() > 0) mediaHandler.setTime(info.start());
                        mediaHandler.setRepeat(type.isLooped);
                    }
                }
            }
            else {
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

        int l = PictureSignConfig.fullBrightPicture ? 15728880 : light;
//        if (PlatformFunctions.isModLoaded("iris") && IrisCompat.isShaderPackInUse())
//            RenderSystem.setShader(PictureSignConfig.pictureShader.program);
        //? if < 1.21.4
        //RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);

        ResourceLocation texture = PictureSignConfig.missingImageMode.missingTextureId;
        long rawTexture = -1;
        if (errorMessage == null) {
            if (type == PICTURE) {
                assert data != null;
                texture = data.identifier;
            } else if (type.isVideo && mediaHandler != null) {
                if (mediaHandler.isWorking()) {
                    rawTexture = mediaHandler.getTexture();
                    if (rawTexture != -1) {
                        texture = RAW_TEXTURE;
                    }
                }
            } else if (gifHandler != null) {
                if (gifHandler.isWorking()) {
                    rawTexture = gifHandler.getTexture();
                    texture = RAW_TEXTURE;
                }
            }
        }

        if (texture == null) return;

        //? if < 1.21.4 {
        /*if (texture != RAW_TEXTURE) RenderSystem.setShaderTexture(0, Minecraft.getInstance().getTextureManager().getTexture(texture).getId());
        else RenderSystem.setShaderTexture(0, rawTexture);
        if (PictureSignConfig.translucency) RenderSystem.enableBlend();
        else RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        *///?}

        Tesselator tessellator = Tesselator.getInstance();

        matrixStack.pushPose();
        matrixStack.translate(offset.xOffset() + dimensions.x(), dimensions.y(), offset.zOffset() + dimensions.z());
        matrixStack.mulPose(Axis.YP.rotationDegrees(offset.yRotation() + dimensions.yRot()));
        matrixStack.mulPose(Axis.XP.rotationDegrees(dimensions.xRot()));
        matrixStack.mulPose(Axis.ZP.rotationDegrees(dimensions.zRot()));

        Matrix4f matrix4f = matrixStack.last().pose();
        //? if >= 1.21.4 {
        RenderType renderLayer = texture != RAW_TEXTURE ? RenderLayerUtil.TEXTURED_RENDER_LAYER.apply(texture) : RenderLayerUtil.GL_RENDER_LAYER.apply(videoId, (int) rawTexture);
        VertexConsumer buffer = vertices.getBuffer(renderLayer);
        //?} else {
        /*BufferBuilder buffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
        *///?}

        buffer.addVertex(matrix4f, dimensions.width(), 0.0F, 1.0F).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setLight(l).setNormal(matrixStack.last(), 0, 0, 0);

        buffer.addVertex(matrix4f, dimensions.width(), dimensions.height(), 1.0F).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setLight(l).setNormal(matrixStack.last(), 0, 0, 0);

        buffer.addVertex(matrix4f, 0.0F, dimensions.height(), 1.0F).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setLight(l).setNormal(matrixStack.last(), 0, 0, 0);

        buffer.addVertex(matrix4f, 0.0F, 0.0F, 1.0F).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setLight(l).setNormal(matrixStack.last(), 0, 0, 0);

        //? if < 1.21.4 {
        /*BufferUploader.drawWithShader(buffer.buildOrThrow());
        *///?}
        tessellator.clear();

        if (errorMessage != null) renderErrorMessage(errorMessage, Minecraft.getInstance().font, matrixStack, vertices, dimensions.width(), dimensions.height());
        matrixStack.popPose();
        //? if < 1.21.4 {
        /*RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        *///?}
    }

    private static void renderErrorMessage(Component error, @NotNull Font textRenderer, @NotNull PoseStack matrices, MultiBufferSource vertices, float width, float height) {
        float scale = Math.min(width, height) / 100;
        matrices.translate(0, height, 1.005f);
        matrices.mulPose(Axis.YP.rotationDegrees(180));
        matrices.mulPose(Axis.ZP.rotationDegrees(180));
        matrices.scale(scale,scale,scale);
        int wrappedY = 0;
        for(Iterator<FormattedCharSequence> textIterator = textRenderer.split(error, Mth.floor(width/scale)).iterator(); textIterator.hasNext(); wrappedY += 9) {
            renderTextWithShadow(textIterator.next(), wrappedY, textRenderer, matrices.last().pose(), vertices);
        }
    }

    private static void renderTextWithShadow(FormattedCharSequence text, int wrappedY, @NotNull Font textRenderer, Matrix4f matrix, MultiBufferSource vertices) {
        textRenderer.drawInBatch(text, 0, wrappedY, 0xFFFFFF, false,  matrix, vertices, Font.DisplayMode.POLYGON_OFFSET, 0, 15728880);
        matrix.translate(0, 0, 0.025f);
        textRenderer.drawInBatch(text, 1, wrappedY + 1, 0x555555, false,  matrix, vertices, Font.DisplayMode.POLYGON_OFFSET, 0, 15728880);
        matrix.translate(0, 0, -0.025f);
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

    public boolean isDisabledViaRedstone(Level world, BlockPos pos) {
        if (world == null) return false;

        BlockState state = world.getBlockState(pos.below()).hasProperty(BlockStateProperties.LIT) ? world.getBlockState(pos.below()) : world.getBlockState(pos.above());
        if (state.hasProperty(BlockStateProperties.LIT)) return state.getValue(BlockStateProperties.LIT).equals(false);

        return false;
    }
}
