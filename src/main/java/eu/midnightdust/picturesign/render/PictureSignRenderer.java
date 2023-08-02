package eu.midnightdust.picturesign.render;

import com.mojang.blaze3d.systems.RenderSystem;
import eu.midnightdust.picturesign.util.*;
import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.joml.*;

import java.net.MalformedURLException;

import static eu.midnightdust.picturesign.PictureSignClient.MOD_ID;

public class PictureSignRenderer {
    private boolean isSafeUrl;
    private boolean hasRotation = false;
    public void render(SignBlockEntity signBlockEntity, MatrixStack matrixStack, int light, int overlay, boolean front) {
        if (signBlockEntity.isRemoved()) return;
        String url = PictureURLUtils.getLink(signBlockEntity, front);
        PictureInfo info = null;
        if (!url.contains("://")) {
            url = "https://" + url;
        }
        if (url.endsWith(".json")) {
            info = PictureURLUtils.infoFromJson(url);
            if (info == null) return;
            url = info.url();
        }
        if (!url.contains("://")) {
            url = "https://" + url;
        }
        if (PictureSignType.isType(signBlockEntity, PictureSignType.PICTURE, front) && !url.contains(".png") && !url.contains(".jpg") && !url.contains(".jpeg")) return;
        if (PictureSignConfig.safeMode) {
            isSafeUrl = false;
            String finalUrl = url;
            PictureSignConfig.safeProviders.forEach(safe -> {
                if (!isSafeUrl) isSafeUrl = finalUrl.startsWith(safe);
            });
            if (!isSafeUrl/* && !url.startsWith("https://youtu.be/") && !url.startsWith("https://youtube.com/") && !url.startsWith("https://www.youtube.com/")*/) return;
        }
        if ((!PictureSignConfig.enableVideoSigns/* || !PlatformFunctions.isModLoaded("videolib")*/) && !PictureSignType.isType(signBlockEntity, PictureSignType.PICTURE, front)) return;
//        if (url.startsWith("https://youtube.com/") || url.startsWith("https://www.youtube.com/watch?v=") || url.startsWith("https://youtu.be/")) {
//            url = url.replace("https://www.", "https://");
//            url = url.replace("youtube.com/watch?v=", PictureSignConfig.invidiousInstance.replace("https://", "").replace("/", "")+"/latest_version?id=");
//            url = url.replace("youtu.be/", PictureSignConfig.invidiousInstance.replace("https://", "").replace("/", "")+"/latest_version?id=");
//        }
        World world = signBlockEntity.getWorld();
        BlockPos pos = signBlockEntity.getPos();
        String videoSuffix = front ? "_f" : "_b";
        Identifier videoId = new Identifier(MOD_ID, pos.getX() + "_" + pos.getY() + "_" + pos.getZ()+videoSuffix);
        if (world != null && ((world.getBlockState(pos.down()).getBlock().equals(Blocks.REDSTONE_TORCH) || world.getBlockState(pos.down()).getBlock().equals(Blocks.REDSTONE_WALL_TORCH))
                && world.getBlockState(pos.down()).get(Properties.LIT).equals(false)
        || (world.getBlockState(pos.up()).getBlock().equals(Blocks.REDSTONE_TORCH) || world.getBlockState(pos.up()).getBlock().equals(Blocks.REDSTONE_WALL_TORCH))
                        && world.getBlockState(pos.up()).get(Properties.LIT).equals(false)))
        {
            VideoHandler.closePlayer(videoId);
            PictureURLUtils.cachedJsonData.remove(url);
            return;
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
        hasRotation = thirdLine.matches("(.*\\d:.*\\d:.*\\d)");
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
        if (PictureSignType.isType(signBlockEntity, PictureSignType.PICTURE, front)) {
            data = PictureDownloader.getInstance().getPicture(url);
            if (data == null || data.identifier == null) return;
        }
        else if (PictureSignType.isType(signBlockEntity, PictureSignType.VIDEO, front) || PictureSignType.isType(signBlockEntity, PictureSignType.LOOPED_VIDEO, front)) {
            VideoHandler.videoPlayers.add(videoId);
            try {
                if (PictureSignType.isType(signBlockEntity, PictureSignType.LOOPED_VIDEO, front) && !VideoHandler.hasMedia(videoId)) {
                    VideoHandler.play(videoId, url);
                    VideoHandler.setRepeat(videoId, true);
                }
                else if (!VideoHandler.hasMedia(videoId) && !VideoHandler.playedOnce.contains(videoId)) {
                    VideoHandler.play(videoId, url);
                }

            } catch (MalformedURLException e) {
                PictureSignClient.LOGGER.error(e);
                return;
            }
            if (info != null && info.start() > 0 && VideoHandler.getTime(videoId) < info.start()) VideoHandler.setTime(videoId, info.start());
            if (info != null && info.end() > 0 && VideoHandler.getTime(videoId) >= info.end() && !VideoHandler.playedOnce.contains(videoId)) VideoHandler.stop(videoId);
        }
        else return;

        if (PictureSignType.isType(signBlockEntity, PictureSignType.VIDEO, front)) VideoHandler.playedOnce.add(videoId);

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
        BufferBuilder buffer = tessellator.getBuffer();

        int l = PictureSignConfig.fullBrightPicture ? 15728880 : light;
        if (FabricLoader.getInstance().isModLoaded("iris") && IrisApi.getInstance().isShaderPackInUse()) {
            RenderSystem.setShader(PictureSignConfig.pictureShader.program);
        }
        else RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapProgram);

        Identifier texture;
        if (PictureSignType.isType(signBlockEntity, PictureSignType.PICTURE, front)) {
            assert data != null;
            texture = data.identifier;
        }
        else if (PictureSignType.isType(signBlockEntity, PictureSignType.VIDEO, front) || PictureSignType.isType(signBlockEntity, PictureSignType.LOOPED_VIDEO, front))
            texture = VideoHandler.getTexture(videoId);
        else return;
        TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
        if (textureManager.getTexture(texture) == null || (textureManager.getTexture(texture) instanceof NativeImageBackedTexture nativeTexture && nativeTexture.getImage() == null) ||
                (!PictureSignType.isType(signBlockEntity, PictureSignType.PICTURE, front) && VideoHandler.getFramerate(videoId) < 1)) {
            if (PictureSignConfig.missingImageMode.equals(PictureSignConfig.MissingImageMode.TRANSPARENT)) return;
            texture = PictureSignConfig.missingImageMode.equals(PictureSignConfig.MissingImageMode.BLACK) ?
                    (new Identifier(MOD_ID, "textures/black.png")) : (TextureManager.MISSING_IDENTIFIER);
        }
        RenderSystem.setShaderTexture(0, texture);

        if (PictureSignConfig.translucency) RenderSystem.enableBlend();
        else RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        matrixStack.push();
        matrixStack.translate(xOffset + x, y, zOffset + z);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yRotation + yRot));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(xRot));
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(zRot));
        //System.out.println(hasRotation +" "+ xRot + " " + yRot + " " + zRot);

        //matrixStack.multiply(new Quaternionf(new AxisAngle4d(Math.toRadians(yRotation + yRot), 0, 1, 0)));

        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);

        buffer.vertex(matrix4f, width, 0.0F, 1.0F).color(255, 255, 255, 255).texture(1.0F, 1.0F).light(l).overlay(overlay)
                .next();

        buffer.vertex(matrix4f, width, height, 1.0F).color(255, 255, 255, 255).texture(1.0F, 0.0F).light(l).overlay(overlay)
                .next();

        buffer.vertex(matrix4f, 0.0F, height, 1.0F).color(255, 255, 255, 255).texture(0.0F, 0.0F).light(l).overlay(overlay)
                .next();

        buffer.vertex(matrix4f, 0.0F, 0.0F, 1.0F).color(255, 255, 255, 255).texture(0.0F, 1.0F).light(l).overlay(overlay)
                .next();

        tessellator.draw();
        matrixStack.pop();
        RenderSystem.disableBlend();

        RenderSystem.disableDepthTest();
    }
}
