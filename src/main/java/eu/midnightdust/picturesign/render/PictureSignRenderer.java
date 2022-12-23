package eu.midnightdust.picturesign.render;

import com.mojang.blaze3d.systems.RenderSystem;
import eu.midnightdust.picturesign.PictureDownloader;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.util.PictureURLUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Arrays;
import java.util.List;

public class PictureSignRenderer {

    public void render(SignBlockEntity signBlockEntity, MatrixStack matrixStack, int light, int overlay) {
        String url = PictureURLUtils.getLink(signBlockEntity);
        if (!url.startsWith("https://") && !url.startsWith("http://") && !url.startsWith("file:") && !url.startsWith("rp:")) {
            url = "https://" + url;
        }
        if (!url.contains(".png") && !url.contains(".jpg") && !url.contains(".jpeg")) return;
        if (PictureSignConfig.safeMode && !url.startsWith("https://i.imgur.com/") && !url.startsWith("https://i.ibb.co/")
                && !url.startsWith("https://pictshare.net/") && !url.startsWith("https://iili.io/"))
            return;
        World world = signBlockEntity.getWorld();
        BlockPos pos = signBlockEntity.getPos();
        if (world != null && (world.getBlockState(pos.down()).getBlock().equals(Blocks.REDSTONE_TORCH) || world.getBlockState(pos.down()).getBlock().equals(Blocks.REDSTONE_WALL_TORCH))
                && world.getBlockState(pos.down()).get(Properties.LIT).equals(false)) return;
        if (world != null && (world.getBlockState(pos.up()).getBlock().equals(Blocks.REDSTONE_TORCH) || world.getBlockState(pos.up()).getBlock().equals(Blocks.REDSTONE_WALL_TORCH))
                && world.getBlockState(pos.up()).get(Properties.LIT).equals(false)) return;


        String lastLine = signBlockEntity.getTextOnRow(3, false).getString();

        if (!lastLine.matches("(.*\\d:.*\\d:.*\\d:.*\\d:.*\\d)")) return;

        List<String> scale = Arrays.stream(lastLine.split(":")).toList();
        float width = 0;
        float height = 0;
        float x = 0;
        float y = 0;
        float z = 0;
        try {
            width = Float.parseFloat(scale.get(0));
            height = Float.parseFloat(scale.get(1));
            x = Float.parseFloat(scale.get(2));
            y = Float.parseFloat(scale.get(3));
            z = Float.parseFloat(scale.get(4));
        }
        catch (NumberFormatException ignored) {}

        // Download the picture data
        PictureDownloader.PictureData data = PictureDownloader.getInstance().getPicture(url);
        if (data == null || data.identifier == null) {
            return;
        }

        float xOffset = 0.0F;
        float zOffset = 0.0F;

        Quaternionf yRotation = RotationAxis.POSITIVE_Y.rotationDegrees(0F);

        if (signBlockEntity.getCachedState().contains(Properties.HORIZONTAL_FACING)) {
            Direction direction = signBlockEntity.getCachedState().get(Properties.HORIZONTAL_FACING);
            switch (direction) {
                case NORTH -> {
                    zOffset = 1.01F;
                    xOffset = 1.0F;
                    yRotation = RotationAxis.POSITIVE_Y.rotationDegrees(180.0F);
                }
                case SOUTH -> zOffset = 0.010F;
                case EAST -> {
                    zOffset = 1.01F;
                    yRotation = RotationAxis.POSITIVE_Y.rotationDegrees(90.0F);
                }
                case WEST -> {
                    yRotation = RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F);
                    xOffset = 1.01F;
                }
            }
        }
        else if (signBlockEntity.getCachedState().contains(Properties.ROTATION)) {
            yRotation = RotationAxis.POSITIVE_Y.rotationDegrees(signBlockEntity.getCachedState().get(Properties.ROTATION) * -22.5f);
        }
        else return;


        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();



        int l;
        if (FabricLoader.getInstance().isModLoaded("iris") && IrisApi.getInstance().isShaderPackInUse()) {
            RenderSystem.setShader(GameRenderer::getRenderTypeCutoutProgram);
            l = 15728880;
        }
        else {
            RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapProgram);
            l = light;
        }
        RenderSystem.setShaderTexture(0, data.identifier);

        if (PictureSignConfig.translucency) RenderSystem.enableBlend();
        else RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        matrixStack.push();
        matrixStack.translate(xOffset + x, 0.00F + y, zOffset + z);
        matrixStack.multiply(yRotation);

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
