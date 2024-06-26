package eu.midnightdust.picturesign.mixin;

import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.render.PictureSignRenderer;
import eu.midnightdust.picturesign.util.PictureSignType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.HangingSignBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HangingSignBlockEntityRenderer.class)
public abstract class MixinHangingSignBlockEntityRenderer implements BlockEntityRenderer<SignBlockEntity> {
    @Unique PictureSignRenderer psRenderer = new PictureSignRenderer();

    @Inject(at = @At("HEAD"), method = "render")
    public void ps$onRender(SignBlockEntity sign, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, int overlay, CallbackInfo ci) {
        if (PictureSignConfig.enabled) {
            if (PictureSignType.isNotOfType(sign, PictureSignType.NONE, true)) psRenderer.render(sign, matrixStack, light, overlay, true);
            if (PictureSignType.isNotOfType(sign, PictureSignType.NONE, false)) psRenderer.render(sign, matrixStack, light, overlay, false);
        }
    }
    @Unique
    @Override
    public int getRenderDistance() {
        return PictureSignConfig.signRenderDistance;
    }
    @Unique
    @Override
    public boolean rendersOutsideBoundingBox(SignBlockEntity sign) {
        return PictureSignConfig.enabled && PictureSignType.hasPicture(sign);
    }
}
