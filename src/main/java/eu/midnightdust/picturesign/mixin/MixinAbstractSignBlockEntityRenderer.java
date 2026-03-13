package eu.midnightdust.picturesign.mixin;

import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.render.PictureSignRenderer;
import eu.midnightdust.picturesign.util.PictureSignType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static eu.midnightdust.picturesign.util.PictureSignType.isCandidate;
import static eu.midnightdust.picturesign.util.PictureSignType.isNotOfType;

import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(AbstractSignRenderer.class)
public abstract class MixinAbstractSignBlockEntityRenderer implements BlockEntityRenderer<SignBlockEntity> {
    @Unique PictureSignRenderer picturesign$psRenderer = new PictureSignRenderer();

    @Inject(at = @At("HEAD"), method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/world/phys/Vec3;)V")
    public void ps$onRender(SignBlockEntity sign, float f, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int light, int overlay, Vec3 vec3d, CallbackInfo ci) {
        if (PictureSignConfig.enabled) {
            for (boolean front : new boolean[]{true, false})
                if (isCandidate(sign, front) && isNotOfType(sign, PictureSignType.NONE, true)) picturesign$psRenderer.render(sign, matrixStack, vertexConsumerProvider, light, overlay, front);
        }
    }
    @Unique
    @Override
    public int getViewDistance() {
        return PictureSignConfig.signRenderDistance;
    }
    @Unique
    @Override
    public boolean shouldRenderOffScreen(SignBlockEntity sign) {
        return PictureSignConfig.enabled && PictureSignType.hasPicture(sign);
    }
}
