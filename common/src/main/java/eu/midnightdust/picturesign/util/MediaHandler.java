package eu.midnightdust.picturesign.util;

import eu.midnightdust.picturesign.config.PictureSignConfig;
import me.srrapero720.watermedia.api.player.SyncBasePlayer;
import me.srrapero720.watermedia.api.player.SyncMusicPlayer;
import me.srrapero720.watermedia.api.player.SyncVideoPlayer;
import me.srrapero720.watermedia.api.url.UrlAPI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static eu.midnightdust.picturesign.PictureSignClient.id;

public class MediaHandler {
    public static Map<Identifier, MediaHandler> mediaPlayers = new HashMap<>();

    public final Identifier id;
    public boolean playbackStarted = false;
    public boolean isDeactivated;
    private SyncBasePlayer player;

    private MediaHandler(Identifier id) {
        this.id = id;
        mediaPlayers.put(id, this);
    }
    public static MediaHandler getOrCreate(Identifier id) {
        if (mediaPlayers.containsKey(id)) return mediaPlayers.get(id);
        else return new MediaHandler(id);
    }

    public void closePlayer() {
        if (player != null) player.release();
        mediaPlayers.remove(id);
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
        URL fixedUrl = UrlAPI.fixURL(url).url;
        System.out.println("Fixed URL: " + fixedUrl);
        this.player = isVideo ? new SyncVideoPlayer(MinecraftClient.getInstance()) : new SyncMusicPlayer();
        mediaPlayers.put(id, this);
        if (player.isBroken()) return;
        player.start(fixedUrl.toString());
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
        return mediaPlayers.containsKey(id) && !mediaPlayers.get(id).player.isBroken();
    }
    public static Identifier getMissingTexture() {
        if (PictureSignConfig.missingImageMode.equals(PictureSignConfig.MissingImageMode.TRANSPARENT)) return null;
        return PictureSignConfig.missingImageMode.equals(PictureSignConfig.MissingImageMode.BLACK) ?
                (id("textures/black.png")) : (TextureManager.MISSING_IDENTIFIER);
    }
}
