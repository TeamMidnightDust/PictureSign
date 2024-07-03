package eu.midnightdust.picturesign.render;

import eu.midnightdust.picturesign.util.records.PictureDimensions;
import eu.midnightdust.picturesign.util.records.PictureOffset;
import eu.midnightdust.picturesign.util.PictureSignType;
import eu.midnightdust.picturesign.util.PictureURLUtils;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

import static eu.midnightdust.picturesign.util.PictureSignType.getType;

public class PictureSignRenderer extends PictureRenderer {
    public void render(SignBlockEntity signBlockEntity, MatrixStack matrixStack, VertexConsumerProvider vertices, int light, int overlay, boolean front) {
        String lastLine = signBlockEntity.getText(front).getMessage(3, false).getString();
        if (!lastLine.matches("(.*\\d:.*\\d:.*\\d:.*\\d:.*\\d)")) return;

        PictureDimensions dimensions = getDimensions(signBlockEntity.getText(front).getMessage(2, false).getString(), lastLine);
        PictureSignType type = getType(signBlockEntity, front);
        super.render(signBlockEntity, type, PictureURLUtils.getLink(signBlockEntity, front), dimensions, getOffset(signBlockEntity, front, type), front, matrixStack, vertices, light, overlay);
    }
    private PictureDimensions getDimensions(String thirdLine, String forthLine) {
        String[] scale = forthLine.split(":");
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
        return new PictureDimensions(width, height, x, y, z, xRot, yRot, zRot);
    }
    private PictureOffset getOffset(BlockEntity blockEntity, boolean front, PictureSignType type) {
        if (type.isAudio) return null;
        float xOffset = 0.0F;
        float zOffset = 0.0F;
        float yRotation = 0;

        if (blockEntity.getCachedState().contains(Properties.HORIZONTAL_FACING)) {
            Direction direction = blockEntity.getCachedState().get(Properties.HORIZONTAL_FACING);
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
        else if (blockEntity.getCachedState().contains(Properties.ROTATION)) {
            yRotation = blockEntity.getCachedState().get(Properties.ROTATION) * -22.5f;
        }
        else return null;
        if (!front) yRotation -= 180f;
        return new PictureOffset(xOffset, zOffset, yRotation);
    }
}
