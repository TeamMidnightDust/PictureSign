package eu.midnightdust.picturesign.util;

import eu.midnightdust.picturesign.config.PictureSignConfig;
import me.srrapero720.watermedia.api.player.SyncBasePlayer;
import me.srrapero720.watermedia.api.player.SyncMusicPlayer;
import me.srrapero720.watermedia.api.player.SyncVideoPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import static eu.midnightdust.picturesign.PictureSignClient.client;
import static eu.midnightdust.picturesign.PictureSignClient.id;

public class MediaHandler {
    public static Map<Identifier, MediaHandler> mediaPlayers = new HashMap<>();

    public final Identifier id;
    public final BlockPos pos;
    public boolean playbackStarted = false;
    public boolean isDeactivated;
    private SyncBasePlayer player;
    private int maxVolume = 100;

    private MediaHandler(Identifier id, BlockPos pos) {
        this.id = id;
        this.pos = pos;
        mediaPlayers.put(id, this);
    }
    public static MediaHandler getOrCreate(Identifier id, BlockPos pos) {
        if (mediaPlayers.containsKey(id)) return mediaPlayers.get(id);
        else return new MediaHandler(id, pos);
    }
    public void setVolumeBasedOnDistance() {
        if (player == null || client.player == null) return;

        Vec3d playerPos = client.player.getPos();
        if (PictureSignConfig.audioDistanceMultiplier == 0) {
            setVolume(0);
            return;
        }
        double distance = this.pos.getSquaredDistance(playerPos) / PictureSignConfig.audioDistanceMultiplier;
        setVolume((int) Math.clamp(maxVolume-distance, 0, 100));
    }
    private void setVolume(int volume) {
        player.setVolume(volume);
    }
    public void setMaxVolume(int volume) {
        maxVolume = volume;
    }

    public void closePlayer() {
        if (player != null) {
            player.stop();
            player.release();
        }
        //mediaPlayers.remove(id);
        player = null;
    }
    public static void closePlayer(Identifier videoId) {
        if (mediaPlayers.containsKey(videoId)) mediaPlayers.get(videoId).closePlayer();
    }
    public static void closeAll() {
        mediaPlayers.forEach(((id, player) -> player.closePlayer()));
        mediaPlayers.clear();
    }
    public void stop() {
        player.stop();
        isDeactivated = true;
    }
    public boolean isStopped() {
        return player.isStopped();
    }
    public boolean isPaused() {
        return player.isPaused();
    }
    public void pause() {
        player.pause();
    }
    public void restart() {
        player.play();
    }

    public void play(String url, boolean isVideo) throws MalformedURLException {
        this.player = isVideo ? new SyncVideoPlayer(MinecraftClient.getInstance()) : new SyncMusicPlayer();
        mediaPlayers.put(id, this);
        if (player.isBroken()) return;
        player.start(url);
        this.playbackStarted = true;
    }
    public boolean hasMedia() {
        return player != null && player.isPlaying();
    }
    public void setRepeat(boolean value) {
        player.setRepeatMode(true);
    }
    public long getTime() {
        return player.getTime();
    }
    public void setTime(long value) {
        player.seekTo(value);
    }
    public int getTexture() {
        if (player instanceof SyncVideoPlayer videoPlayer) return videoPlayer.getGlTexture();
        return -1;
    }
    public boolean isWorking() {
        return mediaPlayers.containsKey(id) && mediaPlayers.get(id).player != null && !mediaPlayers.get(id).player.isBroken();
    }
    public static Identifier getMissingTexture() {
        if (PictureSignConfig.missingImageMode.equals(PictureSignConfig.MissingImageMode.TRANSPARENT)) return null;
        return PictureSignConfig.missingImageMode.equals(PictureSignConfig.MissingImageMode.BLACK) ?
                (id("textures/black.png")) : (TextureManager.MISSING_IDENTIFIER);
    }
}
