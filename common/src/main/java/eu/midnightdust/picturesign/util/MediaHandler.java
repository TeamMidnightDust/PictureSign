package eu.midnightdust.picturesign.util;

import eu.midnightdust.picturesign.config.PictureSignConfig;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static eu.midnightdust.picturesign.PictureSignClient.client;

public abstract class MediaHandler {
    private static final List<BiFunction<Identifier, BlockPos, MediaHandler>> implementations = new ArrayList<>();
    public static Map<Identifier, MediaHandler> mediaHandlers = new HashMap<>();

    public final Identifier id;
    public final BlockPos pos;
    public boolean playbackStarted = false;
    public boolean isDeactivated;
    int maxVolume = 100;

    public MediaHandler(Identifier id, BlockPos pos) {
        this.id = id;
        this.pos = pos;
    }
    public static void registerHandler(BiFunction<Identifier, BlockPos, MediaHandler> handler) {
        implementations.add(handler);
    }
    public static boolean hasValidImplementation() {
        return !implementations.isEmpty();
    }
    public static MediaHandler getOrCreate(Identifier id, BlockPos pos) {
        if (mediaHandlers.containsKey(id)) return mediaHandlers.get(id);
        AtomicReference<MediaHandler> handler = new AtomicReference<>();
        implementations.forEach(impl -> {
            handler.set(impl.apply(id, pos));
        });
        return handler.get();
    }
    public void setVolumeBasedOnDistance() {
        if (!isWorking() || client.player == null) return;

        Vec3d playerPos = client.player.getPos();
        if (PictureSignConfig.audioDistanceMultiplier == 0) {
            setVolume(0);
            return;
        }
        double distance = this.pos.getSquaredDistance(playerPos) / PictureSignConfig.audioDistanceMultiplier;
        setVolume((int) Math.clamp(maxVolume-distance, 0, 100));
    }
    @ApiStatus.Internal
    public void setVolume(int volume) {} // Please use 'setMaxVolume' to adjust the playback volume

    public void setMaxVolume(int volume) {
        maxVolume = volume;
    }

    public void closePlayer() {}

    public static void closePlayer(Identifier videoId) {
        if (mediaHandlers.get(videoId) instanceof MediaHandler mediaHandler) mediaHandler.closePlayer();
    }
    public static void closeAll() {
        mediaHandlers.forEach(((id, player) -> player.closePlayer()));
        mediaHandlers.clear();
    }
    public void stop() {
        isDeactivated = true;
    }
    public boolean isStopped() {
        return false;
    }
    public boolean isPaused() {
        return false;
    }
    public void pause() {}
    public void restart() {}

    public void play(String url, boolean isVideo) {
    }
    public boolean hasMedia() {
        return true;
    }
    public void setRepeat(boolean value) {}
    public long getTime() {
        return -1;
    }
    public void setTime(long value) {
    }
    public int getTexture() {
        return -1;
    }
    public boolean isReady() {
        return false;
    }
    public boolean isWorking() {
        return false;
    }
}
