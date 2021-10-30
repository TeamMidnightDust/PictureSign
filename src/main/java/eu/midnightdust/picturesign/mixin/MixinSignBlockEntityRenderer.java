package eu.midnightdust.picturesign.mixin;

import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.render.PictureSignRenderer;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignBlockEntityRenderer.class)
public abstract class MixinSignBlockEntityRenderer implements BlockEntityRenderer<SignBlockEntity> {
    PictureSignRenderer psRenderer = new PictureSignRenderer();

    @Inject(at = @At("HEAD"), method = "render")
    public void ps$onRender(SignBlockEntity signBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, int overlay, CallbackInfo ci) {
        if (PictureSignConfig.enabled && signBlockEntity.getTextOnRow(0,false).getString().matches("(!PS:.*)")) {
            psRenderer.render(signBlockEntity, matrixStack, light, overlay);
        }
    }
    @Inject(at = @At("HEAD"), method = "shouldRender", cancellable = true)
    private static void shouldRender(SignBlockEntity sign, int signColor, CallbackInfoReturnable<Boolean> cir) {
        if (PictureSignConfig.enabled && sign.getTextOnRow(0,false).getString().matches("(!PS:.*)")) cir.setReturnValue(true);
    }
    @Unique
    @Override
    public int getRenderDistance() {
        return PictureSignConfig.signRenderDistance;
    }
    @Unique
    @Override
    public boolean rendersOutsideBoundingBox(SignBlockEntity sign) {
        return PictureSignConfig.enabled && sign.getTextOnRow(0, false).getString().matches("(!PS:.*)");
    }
}
