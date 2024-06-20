package eu.midnightdust.picturesign.util;

import eu.midnightdust.picturesign.config.PictureSignConfig;
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

public class VideoHandler {
    public static Map<Identifier, SyncVideoPlayer> videoPlayers = new HashMap<>();

    private final Identifier id;
    private SyncVideoPlayer player;

    public VideoHandler(Identifier id) {
        this.id = id;
    }

    public void closePlayer() {
        if (videoPlayers.containsKey(id)) videoPlayers.get(id).release();
        videoPlayers.remove(id);
        player = null;
    }
    public static void closePlayer(Identifier videoId) {
        if (videoPlayers.containsKey(videoId)) videoPlayers.get(videoId).release();
        videoPlayers.remove(videoId);
    }
    public static void closeAll() {
        videoPlayers.forEach(((id, player) -> player.release()));
        videoPlayers.clear();
    }
    public void stop() {
        player.stop();
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

    public void play(String url) throws MalformedURLException {
        URL fixedUrl = UrlAPI.fixURL(url).url;
        System.out.println("Fixed URL: " + fixedUrl);
        this.player = new SyncVideoPlayer(MinecraftClient.getInstance());
        videoPlayers.put(id, player);
        if (player.isBroken()) return;
        player.start(fixedUrl.toString());
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
        return player.getGlTexture();
    }
    public boolean isWorking() {
        return videoPlayers.containsKey(id) && !videoPlayers.get(id).isBroken();
    }
    public static Identifier getMissingTexture() {
        if (PictureSignConfig.missingImageMode.equals(PictureSignConfig.MissingImageMode.TRANSPARENT)) return null;
        return PictureSignConfig.missingImageMode.equals(PictureSignConfig.MissingImageMode.BLACK) ?
                (id("textures/black.png")) : (TextureManager.MISSING_IDENTIFIER);
    }
}
