package eu.midnightdust.picturesign.mixin;

import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.render.PictureSignRenderer;
import eu.midnightdust.picturesign.util.PictureSignType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static eu.midnightdust.picturesign.PictureSignClient.client;
import static eu.midnightdust.picturesign.util.PictureSignType.isCandidate;
import static eu.midnightdust.picturesign.util.PictureSignType.isNotOfType;
import static eu.midnightdust.picturesign.util.PictureSignType.hasPicture;

@Mixin(SignBlockEntityRenderer.class)
public abstract class MixinSignBlockEntityRenderer implements BlockEntityRenderer<SignBlockEntity> {
    @Unique private PictureSignRenderer psRenderer;

    @Inject(at = @At("TAIL"), method = "<init>")
    public void ps$onInit(BlockEntityRendererFactory.Context ctx, CallbackInfo ci) {
        psRenderer = new PictureSignRenderer();
    }

    @Inject(at = @At("HEAD"), method = "render")
    public void ps$onRender(SignBlockEntity sign, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, int overlay, CallbackInfo ci) {
        if (PictureSignConfig.enabled && psRenderer != null) {
            if (isCandidate(sign, true) && isNotOfType(sign, PictureSignType.NONE, true)) psRenderer.render(sign, matrixStack, vertexConsumerProvider, light, overlay, true);
            if (isCandidate(sign, false) && isNotOfType(sign, PictureSignType.NONE, false)) psRenderer.render(sign, matrixStack, vertexConsumerProvider, light, overlay, false);
        }
    }
    @Inject(at = @At("HEAD"), method = "shouldRender", cancellable = true)
    private static void shouldRender(BlockPos pos, int signColor, CallbackInfoReturnable<Boolean> cir) {
        if (PictureSignConfig.enabled && client.world != null && hasPicture((SignBlockEntity) client.world.getBlockEntity(pos))) cir.setReturnValue(true);
    }
    @Unique
    @Override
    public int getRenderDistance() {
        return PictureSignConfig.signRenderDistance;
    }
    @Unique
    @Override
    public boolean rendersOutsideBoundingBox(SignBlockEntity sign) {
        return PictureSignConfig.enabled && hasPicture(sign);
    }
}
