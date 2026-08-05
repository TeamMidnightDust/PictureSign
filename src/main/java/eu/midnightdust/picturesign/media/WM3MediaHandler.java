package eu.midnightdust.picturesign.media;

import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.mixin.RenderSystemAccessor;
import org.watermedia.api.media.MRL;
import org.watermedia.api.media.MediaAPI;
import org.watermedia.api.media.players.MediaPlayer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.net.URI;

public class WM3MediaHandler extends MediaHandler {
    private MRL mrl;
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
    public void load(String url) {
        mrl = MediaAPI.mrl(url);
        mrl.await(1000);

        if (player == null) {
            PictureSignClient.LOGGER.warn("Video/audio player for URL {} could not be created", url);
        }
    }

    @Override
    public void play(boolean isVideo) {
        if (mrl.status() == MRL.Status.LOADED) {
            this.player = MediaAPI.createPlayer(mrl,
                    isVideo ? () -> MediaAPI.glEngine(RenderSystemAccessor.getRenderThread(), Minecraft.getInstance()) : null,
                    MediaAPI::alEngine);
        } else this.player = null;

        if (this.player != null) player.start();
    }

    @Override
    public boolean isPlaying() {
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
    public long getTexture() {
        if (player.withVideo()) {
            return player.texture();
        }
        return -1;
    }

    @Override
    public boolean hasMedia(String url) {
        return mrl != null && mrl.uri.equals(URI.create(url));
    }

    @Override
    public boolean isLoaded() {
        return mrl != null && mrl.status() == MRL.Status.LOADED;
    }

    @Override
    public boolean isWorking() {
        return player != null && !player.error();
    }
}
