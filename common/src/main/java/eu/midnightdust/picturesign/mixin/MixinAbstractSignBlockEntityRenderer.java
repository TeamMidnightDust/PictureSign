package eu.midnightdust.picturesign.mixin;

import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.render.PictureSignRenderer;
import eu.midnightdust.picturesign.util.PictureSignType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.AbstractSignBlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static eu.midnightdust.picturesign.util.PictureSignType.isCandidate;
import static eu.midnightdust.picturesign.util.PictureSignType.isNotOfType;

@Mixin(AbstractSignBlockEntityRenderer.class)
public abstract class MixinAbstractSignBlockEntityRenderer implements BlockEntityRenderer<SignBlockEntity> {
    @Unique PictureSignRenderer picturesign$psRenderer = new PictureSignRenderer();

    @Inject(at = @At("HEAD"), method = "render(Lnet/minecraft/block/entity/SignBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/util/math/Vec3d;)V")
    public void ps$onRender(SignBlockEntity sign, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, int overlay, Vec3d vec3d, CallbackInfo ci) {
        if (PictureSignConfig.enabled) {
            for (boolean front : new boolean[]{true, false})
                if (isCandidate(sign, front) && isNotOfType(sign, PictureSignType.NONE, true)) picturesign$psRenderer.render(sign, matrixStack, vertexConsumerProvider, light, overlay, front);
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
