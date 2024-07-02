package eu.midnightdust.picturesign.render;

import com.mojang.blaze3d.systems.RenderSystem;
import eu.midnightdust.lib.util.PlatformFunctions;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.util.GIFHandler;
import eu.midnightdust.picturesign.util.IrisCompat;
import eu.midnightdust.picturesign.util.MediaHandler;
import eu.midnightdust.picturesign.util.PictureDownloader;
import eu.midnightdust.picturesign.util.PictureInfo;
import eu.midnightdust.picturesign.util.PictureSignType;
import eu.midnightdust.picturesign.util.PictureURLUtils;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Iterator;

import static eu.midnightdust.picturesign.PictureSignClient.client;
import static eu.midnightdust.picturesign.PictureSignClient.hasWaterMedia;
import static eu.midnightdust.picturesign.PictureSignClient.id;
import static eu.midnightdust.picturesign.util.PictureSignType.GIF;
import static eu.midnightdust.picturesign.util.PictureSignType.PICTURE;
import static eu.midnightdust.picturesign.util.PictureSignType.getType;

public class PictureSignRenderer {
    private boolean isSafeUrl;
    private boolean isSafeJsonUrl;

    @Nullable private Text errorMessage;
    public static final Text ERROR = Text.translatable("picturesign.error");
    public static final Text MISSING_VLC = ERROR.copy().append(Text.translatable("picturesign.error.missingVLC"));
    public static final Text MISSING_WATERMEDIA = ERROR.copy().append(Text.translatable("picturesign.error.missingWatermedia"));
    public static final Text IMAGE_NOT_FOUND = ERROR.copy().append(Text.translatable("picturesign.error.imageNotFound"));
    public static final Text UNKNOWN_FILETYPE = ERROR.copy().append(Text.translatable("picturesign.error.unknownFiletype"));
    public static final Text UNKNOWN_SIGNTYPE = ERROR.copy().append(Text.translatable("picturesign.error.unknownSigntype"));
    public static final Text UNSAFE_JSON_URL = ERROR.copy().append(Text.translatable("picturesign.error.unsafeJsonUrl"));
    public static final Text UNSAFE_URL = ERROR.copy().append(Text.translatable("picturesign.error.unsafeUrl"));

    public static final Identifier RAW_TEXTURE = id("internal_raw_texture");

    public void render(SignBlockEntity signBlockEntity, MatrixStack matrixStack, VertexConsumerProvider vertices, int light, int overlay, boolean front) {
        errorMessage = null;
        PictureSignType type = getType(signBlockEntity, front);
        String url = PictureURLUtils.getLink(signBlockEntity, front);
        PictureInfo info = null;
        if (!url.contains("://") && !url.startsWith("file:") && !url.startsWith("rp:")) {
            url = "https://" + url;
        }
        isSafeJsonUrl = false;
        String jsonUrl = url;
        PictureSignConfig.safeJsonProviders.forEach(safe -> {
            if (!isSafeJsonUrl) isSafeJsonUrl = jsonUrl.startsWith(safe);
        });
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

        if (type == PICTURE && !url.contains(".png") && !url.contains(".jpg") && !url.contains(".jpeg") && !url.startsWith("rp:")) errorMessage = UNKNOWN_FILETYPE;
        if (type == GIF && !url.contains(".gif")) errorMessage = UNKNOWN_FILETYPE;
        if (PictureSignConfig.safeMode && !url.startsWith("file:") && !url.startsWith("rp:")) {
            isSafeUrl = false;
            String finalUrl = url;
            if (type == PICTURE) {
                PictureSignConfig.safeProviders.forEach(safe -> {
                    if (!isSafeUrl) isSafeUrl = finalUrl.startsWith(safe);
                });
            }
            if (type == GIF) {
                PictureSignConfig.safeGifProviders.forEach(safe -> {
                    if (!isSafeUrl) isSafeUrl = finalUrl.startsWith(safe);
                });
            }
            else if (type.isVideo || type.isAudio) {
                PictureSignConfig.safeMultimediaProviders.forEach(safe -> {
                    if (!isSafeUrl) isSafeUrl = finalUrl.startsWith(safe);
                });
            }
            if (!isSafeUrl) errorMessage = UNSAFE_URL;
        }
        if ((!PictureSignConfig.enableMultimediaSigns || !MediaHandler.hasValidImplementation()) && type != PICTURE) errorMessage = MISSING_WATERMEDIA;

        if (url.startsWith("https://youtube.com/") || url.startsWith("https://www.youtube.com/watch?v=") || url.startsWith("https://youtu.be/")) {
            url = url.replace("https://www.", "https://");
        }
        World world = signBlockEntity.getWorld();
        BlockPos pos = signBlockEntity.getPos();
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

        if (world != null && ((world.getBlockState(pos.down()).getBlock().equals(Blocks.REDSTONE_TORCH) || world.getBlockState(pos.down()).getBlock().equals(Blocks.REDSTONE_WALL_TORCH))
                && world.getBlockState(pos.down()).get(Properties.LIT).equals(false)
        || (world.getBlockState(pos.up()).getBlock().equals(Blocks.REDSTONE_TORCH) || world.getBlockState(pos.up()).getBlock().equals(Blocks.REDSTONE_WALL_TORCH))
                        && world.getBlockState(pos.up()).get(Properties.LIT).equals(false)))
        {
            if (mediaHandler != null && mediaHandler.isWorking() && !mediaHandler.isStopped()) {
                mediaHandler.stop();
            }

            PictureURLUtils.cachedJsonData.remove(url);
            return;
        }
        else if (mediaHandler != null && mediaHandler.isDeactivated) {
            if (mediaHandler.isWorking() && mediaHandler.isStopped())
                mediaHandler.restart();
        }

        String lastLine = signBlockEntity.getText(front).getMessage(3, false).getString();

        if (!lastLine.matches("(.*\\d:.*\\d:.*\\d:.*\\d:.*\\d)")) return;

        String[] scale = lastLine.split(":");
        float width = 0;
        float height = 0;
        float x = 0;
        float y = 0;
        float z = 0;
        try {
            width = Float.parseFloat(scale[0]);
            height = Float.parseFloat(scale[1]);
            x = Float.parseFloat(scale[2]);
            y = Float.parseFloat(scale[3]);
            z = Float.parseFloat(scale[4]);
        }
        catch (NumberFormatException ignored) {}

        String thirdLine = signBlockEntity.getText(front).getMessage(2, false).getString();
        boolean hasRotation = thirdLine.matches("(.*\\d:.*\\d:.*\\d)");
        float xRot = 0;
        float yRot = 0;
        float zRot = 0;

        if (hasRotation) {
            String[] rotation = thirdLine.split(":");
            try {
                xRot = Float.parseFloat(rotation[0]);
                yRot = Float.parseFloat(rotation[1]);
                zRot = Float.parseFloat(rotation[2]);
            } catch (NumberFormatException ignored) {
            }
        }

        // Download the picture data
        PictureDownloader.PictureData data = null;
        if (errorMessage == null && type == PICTURE) {
            data = PictureDownloader.getInstance().getPicture(url);
            if (data == null || data.identifier == null) errorMessage = IMAGE_NOT_FOUND;
        }
        else if (mediaHandler != null) {
            if (!mediaHandler.isReady()) errorMessage = MISSING_VLC;
            else {
                if (!mediaHandler.hasMedia() && !mediaHandler.playbackStarted) {
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

        if (type.isAudio) return;

        float xOffset = 0.0F;
        float zOffset = 0.0F;

        float yRotation = 0;

        if (signBlockEntity.getCachedState().contains(Properties.HORIZONTAL_FACING)) {
            Direction direction = signBlockEntity.getCachedState().get(Properties.HORIZONTAL_FACING);
            switch (direction) {
                case NORTH -> {
                    zOffset = 1.01F;
                    xOffset = 1.0F;
                    yRotation = 180;
                }
                case SOUTH -> zOffset = 0.010F;
                case EAST -> {
                    zOffset = 1.01F;
                    yRotation = 90;
                }
                case WEST -> {
                    yRotation = -90;
                    xOffset = 1.01F;
                }
            }
        }
        else if (signBlockEntity.getCachedState().contains(Properties.ROTATION)) {
            yRotation = signBlockEntity.getCachedState().get(Properties.ROTATION) * -22.5f;
        }
        else return;
        if (!front) yRotation -= 180f;

        Tessellator tessellator = Tessellator.getInstance();

        int l = PictureSignConfig.fullBrightPicture ? 15728880 : light;
        if (PlatformFunctions.isModLoaded("iris") && IrisCompat.isShaderPackInUse()) {
            RenderSystem.setShader(PictureSignConfig.pictureShader.program);
        }
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
        matrixStack.translate(xOffset + x, y, zOffset + z);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yRotation + yRot));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(xRot));
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(zRot));

        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);

        buffer.vertex(matrix4f, width, 0.0F, 1.0F).color(255, 255, 255, 255).texture(1.0F, 1.0F).light(l).overlay(overlay);

        buffer.vertex(matrix4f, width, height, 1.0F).color(255, 255, 255, 255).texture(1.0F, 0.0F).light(l).overlay(overlay);

        buffer.vertex(matrix4f, 0.0F, height, 1.0F).color(255, 255, 255, 255).texture(0.0F, 0.0F).light(l).overlay(overlay);

        buffer.vertex(matrix4f, 0.0F, 0.0F, 1.0F).color(255, 255, 255, 255).texture(0.0F, 1.0F).light(l).overlay(overlay);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        if (errorMessage != null) renderErrorMessage(client.textRenderer, matrixStack, vertices, width, height);
        matrixStack.pop();
        RenderSystem.disableBlend();

        RenderSystem.disableDepthTest();
    }
    private void renderErrorMessage(TextRenderer textRenderer, MatrixStack matrices, VertexConsumerProvider vertices, float width, float height) {
        float scale = Math.min(width, height) / 100;
        matrices.translate(0, height, 1.005f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));
        matrices.scale(scale,scale,scale);
        int wrappedY = 0;
        for(Iterator<OrderedText> textIterator = textRenderer.wrapLines(errorMessage, MathHelper.floor(width/scale)).iterator(); textIterator.hasNext(); wrappedY += 9) {
            renderTextWithShadow(textIterator.next(), wrappedY, textRenderer, matrices.peek().getPositionMatrix(), vertices);
        }
    }
    private void renderTextWithShadow(OrderedText text, int wrappedY, TextRenderer textRenderer, Matrix4f matrix, VertexConsumerProvider vertices) {
        textRenderer.draw(text, 0, wrappedY, 0xFFFFFF, false,  matrix, vertices, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, 15728880);
        matrix.translate(0, 0, 0.025f);
        textRenderer.draw(text, 1, wrappedY + 1, 0x555555, false,  matrix, vertices, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, 15728880);
        matrix.translate(0, 0, -0.025f);
    }
    public static Identifier getMissingTexture() {
        if (PictureSignConfig.missingImageMode.equals(PictureSignConfig.MissingImageMode.TRANSPARENT)) return null;
        return PictureSignConfig.missingImageMode.equals(PictureSignConfig.MissingImageMode.BLACK) ?
                (id("textures/black.png")) : (TextureManager.MISSING_IDENTIFIER);
    }
}
