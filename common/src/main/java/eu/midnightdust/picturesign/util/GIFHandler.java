package eu.midnightdust.picturesign.util;

import me.srrapero720.watermedia.api.image.ImageAPI;
import me.srrapero720.watermedia.api.image.ImageCache;
import me.srrapero720.watermedia.api.math.MathAPI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import static eu.midnightdust.picturesign.PictureSignClient.client;

public class GIFHandler {
    public static Map<Identifier, GIFHandler> gifPlayers = new HashMap<>();

    public final Identifier id;
    public boolean playbackStarted;
    private ImageCache player;
    private long tick = 0L;

    private GIFHandler(Identifier id) {
        System.out.println("New GIF handler :" + id);
        this.id = id;
        gifPlayers.put(id, this);
    }
    public static GIFHandler getOrCreate(Identifier id) {
        if (gifPlayers.containsKey(id)) return gifPlayers.get(id);
        else return new GIFHandler(id);
    }
    public void tick() {
        if (player != null && player.getRenderer() != null && tick < player.getRenderer().duration) tick += 1;
        else tick = 0;
    }

    public void closePlayer() {
        player.release();
        player = null;
        gifPlayers.remove(this.id);
    }
    public static void closePlayer(Identifier videoId) {
        if (gifPlayers.containsKey(videoId)) gifPlayers.get(videoId).closePlayer();
    }
    public static void closeAll() {
        gifPlayers.forEach((id, handler) -> handler.closePlayer());
        gifPlayers.clear();
    }

    public void play(String url) throws MalformedURLException {
        this.player = ImageAPI.getCache(url, MinecraftClient.getInstance());
        player.load();
        this.playbackStarted = true;
    }
    public boolean hasMedia() {
        return player != null && player.getStatus() == ImageCache.Status.READY;
    }
    public int getTexture() {
        return player.getRenderer().texture(tick,
                (MathAPI.tickToMs(client.getRenderTickCounter().getTickDelta(true))), true);
    }
    public boolean isWorking() {
        if (player != null && player.getException() != null) player.getException().fillInStackTrace();
        return player != null && player.getStatus() == ImageCache.Status.READY && player.getRenderer() != null;
    }
}
