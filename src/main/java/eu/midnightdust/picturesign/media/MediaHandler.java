package eu.midnightdust.picturesign.media;

import eu.midnightdust.picturesign.config.PictureSignConfig;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public abstract class MediaHandler {
    private static final List<BiFunction<ResourceLocation, BlockPos, MediaHandler>> implementations = new ArrayList<>();
    public static Map<ResourceLocation, MediaHandler> mediaHandlers = new HashMap<>();

    public final ResourceLocation id;
    public final BlockPos pos;
    public boolean playbackStarted = false;
    public boolean isDeactivated;
    int maxVolume = 100;

    public MediaHandler(ResourceLocation id, BlockPos pos) {
        this.id = id;
        this.pos = pos;
    }

    public static void registerHandler(BiFunction<ResourceLocation, BlockPos, MediaHandler> handler) {
        implementations.add(handler);
    }

    public static boolean hasValidImplementation() {
        return !implementations.isEmpty();
    }

    public static MediaHandler getOrCreate(ResourceLocation id, BlockPos pos) {
        if (mediaHandlers.containsKey(id))
            return mediaHandlers.get(id);
        AtomicReference<MediaHandler> handler = new AtomicReference<>();
        implementations.forEach(impl -> {
            handler.set(impl.apply(id, pos));
        });
        return handler.get();
    }

    public final void setVolumeBasedOnDistance() {
        if (!isWorking() || Minecraft.getInstance().player == null) return;

        Vec3 playerPos = Minecraft.getInstance().player.position();
        if (PictureSignConfig.audioDistanceMultiplier == 0) {
            setVolume(0);
            return;
        }
        double distance = this.pos.distToCenterSqr(playerPos) / PictureSignConfig.audioDistanceMultiplier;
        setVolume((int) Math.clamp(maxVolume-distance, 0, 100));
    }

    public final void setMaxVolume(int volume) {
        maxVolume = volume;
    }

    public static void closePlayer(ResourceLocation videoId) {
        if (mediaHandlers.get(videoId) instanceof MediaHandler mediaHandler)
            mediaHandler.closePlayer();
    }

    public static void closeAll() {
        mediaHandlers.forEach(((id, player) -> player.closePlayer()));
        mediaHandlers.clear();
    }

    public void stop() {
        isDeactivated = true;
    }

    public abstract boolean isStopped();

    public abstract boolean isPaused();

    public abstract void pause();

    public abstract void restart();

    public abstract void play(String url, boolean isVideo);

    public abstract void closePlayer();

    public abstract boolean hasMedia();

    public abstract void setRepeat(boolean value);

    public abstract long getTime();

    public abstract void setTime(long value);

    public abstract int getTexture();

    public abstract boolean isReady();

    public abstract boolean isWorking();

    @ApiStatus.Internal
    public void setVolume(int volume) {} // Please use 'setMaxVolume' to adjust the playback volume
}
