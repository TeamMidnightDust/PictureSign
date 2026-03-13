package eu.midnightdust.picturesign.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.watermedia.api.media.MediaAPI;
import org.watermedia.api.media.platform.IPlatform;

import java.util.LinkedList;

@Mixin(value = MediaAPI.class, remap = false)
public interface MediaAPIAccessor {
    @Accessor("PLATFORMS")
    static LinkedList<IPlatform> getPlatforms() {
        return null;
    }
}
