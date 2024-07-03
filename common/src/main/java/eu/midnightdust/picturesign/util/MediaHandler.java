package eu.midnightdust.picturesign.util;

import eu.midnightdust.picturesign.config.PictureSignConfig;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

import static eu.midnightdust.picturesign.PictureSignClient.client;
import static eu.midnightdust.picturesign.PictureSignClient.hasWaterMedia;

public abstract class MediaHandler {
    public static Map<Identifier, MediaHandler> mediaHandlers = new HashMap<>();

    public final Identifier id;
    public final BlockPos pos;
    public boolean playbackStarted = false;
    public boolean isDeactivated;
    int maxVolume = 100;

    MediaHandler(Identifier id, BlockPos pos) {
        this.id = id;
        this.pos = pos;
    }
    public static MediaHandler getOrCreate(Identifier id, BlockPos pos) {
        if (mediaHandlers.containsKey(id)) return mediaHandlers.get(id);
        else if (hasWaterMedia) return new WaterMediaHandler(id, pos);
        // Add new implementations here via Mixin
        else return null;
    }
    public static boolean hasValidImplementation() { // Mixin here to add new Multimedia implementations
        if (hasWaterMedia) return true;
        else return false;
    }
    public void setVolumeBasedOnDistance() {
        if (!isWorking() || client.player == null) return;

        Vec3d playerPos = client.player.getPos();
        if (PictureSignConfig.audioDistanceMultiplier == 0) {
            setVolume(0);
            return;
        }
        double distance = this.pos.getSquaredDistance(playerPos) / PictureSignConfig.audioDistanceMultiplier;
        setVolume((int) MathHelper.clamp(maxVolume-distance, 0, 100));
    }
    void setVolume(int volume) {}
    public void setMaxVolume(int volume) {
        maxVolume = volume;
    }

    public void closePlayer() {}

    public static void closePlayer(Identifier videoId) {
        MediaHandler mediaHandler = mediaHandlers.getOrDefault(videoId, null);
        if (mediaHandler != null) mediaHandler.closePlayer();
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
