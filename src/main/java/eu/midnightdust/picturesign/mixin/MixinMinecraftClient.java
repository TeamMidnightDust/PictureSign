package eu.midnightdust.picturesign.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import eu.midnightdust.picturesign.util.VideoHandler;
import net.minecraft.client.MinecraftClient;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Inject(at=@At("HEAD"), method="render")
    public void picturesign$updateVideos(boolean b, CallbackInfo ci) {
        VideoHandler.videoManager.update();
    }
    
}
