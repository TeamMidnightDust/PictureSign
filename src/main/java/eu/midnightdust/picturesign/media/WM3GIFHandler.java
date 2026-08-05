package eu.midnightdust.picturesign.media;

import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.mixin.RenderSystemAccessor;
import net.minecraft.client.Minecraft;

import net.minecraft.resources.ResourceLocation;
import org.watermedia.api.media.MRL;
import org.watermedia.api.media.MediaAPI;
import org.watermedia.api.media.players.MediaPlayer;

public class WM3GIFHandler extends GIFHandler {
    private MediaPlayer player;

    public WM3GIFHandler(ResourceLocation id) {
        super(id);
    }

    @Override
    public void closePlayer() {
        player.release();
        player = null;
        super.closePlayer();
    }

    @Override
    public void play(String url) {
        MRL mrl = MediaAPI.mrl(url);
        mrl.await(1000);
        if (mrl.status() == MRL.Status.LOADED) {
            this.player = MediaAPI.createPlayer(mrl,
                    () -> MediaAPI.glEngine(RenderSystemAccessor.getRenderThread(), Minecraft.getInstance()),
                    null);
        } else this.player = null;
        if (player != null) {
            player.repeat(true);
            player.start();
        } else {
            PictureSignClient.LOGGER.warn("GIF player for URL {} could not be created", url);
        }
        super.play(url);
    }
    @Override
    public boolean hasMedia() {
        return player != null && player.canPlay();
    }
    @Override
    public int getTexture() {
        return (int) player.texture();
    }
    @Override
    public boolean isWorking() {
        return player != null && player.status() != MediaPlayer.Status.ERROR;
    }
}
