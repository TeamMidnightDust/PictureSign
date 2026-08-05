package eu.midnightdust.picturesign.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.render.PictureSignRenderer;
import eu.midnightdust.picturesign.util.PictureSignType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static eu.midnightdust.picturesign.util.PictureSignType.isCandidate;
import static eu.midnightdust.picturesign.util.PictureSignType.isNotOfType;
import static eu.midnightdust.picturesign.util.PictureSignType.hasPicture;

@Mixin(SignRenderer.class)
public abstract class MixinSignBlockEntityRenderer implements BlockEntityRenderer<SignBlockEntity> {
    //? if < 1.21.4 {
    /*@Unique private PictureSignRenderer psRenderer;

    @Inject(at = @At("TAIL"), method = "<init>")
    public void ps$onInit(BlockEntityRendererProvider.Context ctx, CallbackInfo ci) {
        psRenderer = new PictureSignRenderer();
    }

    @Inject(at = @At("HEAD"), method = "render")
    public void ps$onRender(SignBlockEntity sign, float f, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int light, int overlay, CallbackInfo ci) {
        if (PictureSignConfig.enabled && psRenderer != null) {
            if (isCandidate(sign, true) && isNotOfType(sign, PictureSignType.NONE, true)) psRenderer.render(sign, matrixStack, vertexConsumerProvider, light, overlay, true);
            if (isCandidate(sign, false) && isNotOfType(sign, PictureSignType.NONE, false)) psRenderer.render(sign, matrixStack, vertexConsumerProvider, light, overlay, false);
        }
    }

    @Inject(at = @At("HEAD"), method = "isOutlineVisible", cancellable = true)
    private static void shouldRender(BlockPos pos, int signColor, CallbackInfoReturnable<Boolean> cir) {
        if (PictureSignConfig.enabled && Minecraft.getInstance().level != null && hasPicture((SignBlockEntity) Minecraft.getInstance().level.getBlockEntity(pos))) cir.setReturnValue(true);
    }
    *///?}

    @Unique
    @Override
    public int getViewDistance() {
        return PictureSignConfig.signRenderDistance;
    }
}
