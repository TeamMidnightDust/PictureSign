package eu.midnightdust.picturesign.mixin;

import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.render.PictureSignRenderer;
import eu.midnightdust.picturesign.util.PictureSignType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignBlockEntityRenderer.class)
public abstract class MixinSignBlockEntityRenderer implements BlockEntityRenderer<SignBlockEntity> {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    PictureSignRenderer psRenderer = new PictureSignRenderer();

    @Inject(at = @At("HEAD"), method = "render")
    public void ps$onRender(SignBlockEntity sign, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, int overlay, CallbackInfo ci) {
        if (PictureSignConfig.enabled) {
            if (!PictureSignType.isType(sign, PictureSignType.NONE, true)) psRenderer.render(sign, matrixStack, light, overlay, true);
            if (!PictureSignType.isType(sign, PictureSignType.NONE, false)) psRenderer.render(sign, matrixStack, light, overlay, false);
        }
    }
    @Inject(at = @At("HEAD"), method = "shouldRender", cancellable = true)
    private static void shouldRender(BlockPos pos, int signColor, CallbackInfoReturnable<Boolean> cir) {
        if (PictureSignConfig.enabled && client.world != null && !PictureSignType.hasNoPicture((SignBlockEntity) client.world.getBlockEntity(pos))) cir.setReturnValue(true);
    }
    @Unique
    @Override
    public int getRenderDistance() {
        return PictureSignConfig.signRenderDistance;
    }
    @Unique
    @Override
    public boolean rendersOutsideBoundingBox(SignBlockEntity sign) {
        return PictureSignConfig.enabled && !PictureSignType.hasNoPicture(sign);
    }
}
