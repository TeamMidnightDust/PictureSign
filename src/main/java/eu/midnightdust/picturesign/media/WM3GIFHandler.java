package eu.midnightdust.picturesign.media;

import com.mojang.blaze3d.opengl.GlStateManager;
import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.mixin.RenderSystemAccessor;
import net.minecraft.client.Minecraft;

import net.minecraft.resources.ResourceLocation;
import org.watermedia.api.media.MRL;
import org.watermedia.api.media.MediaAPI;
import org.watermedia.api.media.engines.GLEngine;
import org.watermedia.api.media.players.MediaPlayer;

public class WM3GIFHandler extends GIFHandler {
    // ENGINE BUILDERS - Configured once, built per-player
    private static final GLEngine.Builder GL_BUILDER = new GLEngine.Builder()
            .setGenTexture(GlStateManager::_genTexture)
            .setBindTexture((target, tex) -> GlStateManager._bindTexture(tex))
            .setTexParameter(GlStateManager::_texParameter)
            .setPixelStore(GlStateManager::_pixelStore)
            .setDelTexture(GlStateManager::_deleteTexture);

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
        MRL mrl = MediaAPI.getMRL(url);
        while (mrl.busy()) {} // TODO: Replace this dirty hack
        this.player = mrl.createPlayer(RenderSystemAccessor.getRenderThread(), Minecraft.getInstance(), GL_BUILDER.build(), null, true, false);
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
        return player.texture();
    }
    @Override
    public boolean isWorking() {
        return player != null && player.status() != MediaPlayer.Status.ERROR;
    }
}
