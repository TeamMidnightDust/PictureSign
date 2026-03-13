package eu.midnightdust.picturesign.media;

import com.mojang.blaze3d.opengl.GlStateManager;
import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.mixin.RenderSystemAccessor;
import org.watermedia.api.media.MRL;
import org.watermedia.api.media.MediaAPI;
import org.watermedia.api.media.engines.ALEngine;
import org.watermedia.api.media.engines.GLEngine;
import org.watermedia.api.media.players.MediaPlayer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

public class WM3MediaHandler extends MediaHandler {
    // ENGINE BUILDERS - Configured once, built per-player
    private static final GLEngine.Builder GL_BUILDER = new GLEngine.Builder()
            .setGenTexture(GlStateManager::_genTexture)
            .setBindTexture((target, tex) -> GlStateManager._bindTexture(tex))
            .setTexParameter(GlStateManager::_texParameter)
            .setPixelStore(GlStateManager::_pixelStore)
            .setDelTexture(GlStateManager::_deleteTexture);

    private static final ALEngine.Builder AL_BUILDER = new ALEngine.Builder();

    private MediaPlayer player;

    public WM3MediaHandler(ResourceLocation id, BlockPos pos) {
        super(id, pos);
        mediaHandlers.put(id, this);
    }
    @Override
    public void setVolume(int volume) {
        player.volume((int) (volume * Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER)));
    }

    @Override
    public void closePlayer() {
        if (player != null) {
            player.stop();
            player.release();
        }
        //mediaPlayers.remove(id);
        player = null;
    }
    @Override
    public void stop() {
        player.stop();
        super.stop();
    }
    @Override
    public boolean isStopped() {
        return player.status() == MediaPlayer.Status.STOPPED;
    }
    @Override
    public boolean isPaused() {
        return player.status() == MediaPlayer.Status.PAUSED;
    }
    @Override
    public void pause() {
        player.pause();
    }
    @Override
    public void restart() {
        player.start();
    }

    @Override
    public void play(String url, boolean isVideo) {
        MRL mrl = MediaAPI.getMRL(url);
        while (mrl.busy()) {} // TODO: Replace this dirty hack
        this.player = mrl.createPlayer(RenderSystemAccessor.getRenderThread(), Minecraft.getInstance(), GL_BUILDER.build(), AL_BUILDER.build(), isVideo, true);
        if (player != null) {
            player.start();
            //System.out.println("Video FPS: " + player.fps());
        } else {
            PictureSignClient.LOGGER.warn("Video/audio player for URL {} could not be created", url);
        }

        this.playbackStarted = true;
    }
    @Override
    public boolean hasMedia() {
        return player != null && player.playing();
    }
    @Override
    public void setRepeat(boolean value) {
        player.repeat(value);
    }
    @Override
    public long getTime() {
        return player.time();
    }
    @Override
    public void setTime(long value) {
        player.seek(value);
    }
    @Override
    public int getTexture() {
        if (player.withVideo()) {
            return player.texture();
        }
        return -1;
    }
    @Override
    public boolean isReady() {
        return true;//player != null;
    }
    @Override
    public boolean isWorking() {
        return mediaHandlers.containsKey(id) && mediaHandlers.get(id) instanceof WM3MediaHandler WM3MediaHandler
                && WM3MediaHandler.player != null && !WM3MediaHandler.player.error();
    }
}
