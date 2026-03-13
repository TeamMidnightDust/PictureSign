package eu.midnightdust.picturesign.media;

//import org.watermedia.api.image.ImageAPI;
//import org.watermedia.api.image.ImageCache;
//import org.watermedia.api.math.MathAPI;
//
//import java.net.URI;
//import net.minecraft.resources.ResourceLocation;
//
//import static eu.midnightdust.picturesign.PictureSignClient.client;
//
//public class WM2GIFHandler extends GIFHandler {
//    private ImageCache player;
//    private int tick = 0;
//
//    public WM2GIFHandler(ResourceLocation id) {
//        super(id);
//    }
//    @Override
//    public void tick() {
//        if (player != null && player.getRenderer() != null && tick < player.getRenderer().duration) tick += 1;
//        else tick = 0;
//    }
//
//    @Override
//    public void closePlayer() {
//        player.release();
//        player = null;
//        super.closePlayer();
//    }
//
//    @Override
//    public void play(String url) {
//        this.player = ImageAPI.getCache(URI.create(url), client);
//        player.load();
//        super.play(url);
//    }
//    @Override
//    public boolean hasMedia() {
//        return player != null && player.getStatus() == ImageCache.Status.READY;
//    }
//    @Override
//    public int getTexture() {
//        return player.getRenderer().texture(tick, client.getDeltaTracker().getGameTimeDeltaPartialTick(true), true);
//    }
//    @Override
//    public boolean isWorking() {
//        if (player != null && player.getException() != null) player.getException().fillInStackTrace();
//        return player != null && player.getStatus() == ImageCache.Status.READY && player.getRenderer() != null;
//    }
//}
