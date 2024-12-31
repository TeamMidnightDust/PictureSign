package eu.midnightdust.picturesign.util;

import net.minecraft.util.Identifier;
import org.watermedia.api.image.ImageAPI;
import org.watermedia.api.image.ImageCache;
import org.watermedia.api.math.MathAPI;

import java.net.URI;

import static eu.midnightdust.picturesign.PictureSignClient.client;

public class WaterGIFHandler extends GIFHandler {
    private ImageCache player;
    private int tick = 0;

    public WaterGIFHandler(Identifier id) {
        super(id);
    }
    @Override
    public void tick() {
        if (player != null && player.getRenderer() != null && tick < player.getRenderer().duration) tick += 1;
        else tick = 0;
    }

    @Override
    public void closePlayer() {
        player.release();
        player = null;
        super.closePlayer();
    }

    @Override
    public void play(String url) {
        this.player = ImageAPI.getCache(URI.create(url), client);
        player.load();
        super.play(url);
    }
    @Override
    public boolean hasMedia() {
        return player != null && player.getStatus() == ImageCache.Status.READY;
    }
    @Override
    public int getTexture() {
        return player.getRenderer().texture(tick,
                (MathAPI.tickToMs(client.getRenderTickCounter().getTickDelta(true))), true);
    }
    @Override
    public boolean isWorking() {
        if (player != null && player.getException() != null) player.getException().fillInStackTrace();
        return player != null && player.getStatus() == ImageCache.Status.READY && player.getRenderer() != null;
    }
}
