package eu.midnightdust.picturesign.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;
import org.watermedia.api.player.PlayerAPI;
import org.watermedia.api.player.videolan.BasePlayer;
import org.watermedia.api.player.videolan.MusicPlayer;
import org.watermedia.api.player.videolan.VideoPlayer;

import java.net.URI;

import static eu.midnightdust.picturesign.PictureSignClient.client;

public class WaterMediaHandler extends MediaHandler {
    private BasePlayer player;

    public WaterMediaHandler(Identifier id, BlockPos pos) {
        super(id, pos);
        mediaHandlers.put(id, this);
    }
    @Override
    public void setVolume(int volume) {
        player.setVolume((int) (volume * MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.MASTER)));
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
        return player.isStopped();
    }
    @Override
    public boolean isPaused() {
        return player.isPaused();
    }
    @Override
    public void pause() {
        player.pause();
    }
    @Override
    public void restart() {
        player.play();
    }

    @Override
    public void play(String url, boolean isVideo) {
        this.player = isVideo ? new VideoPlayer(client) : new MusicPlayer();
        mediaHandlers.put(id, this);
        if (player.isBroken()) return;
        player.start(URI.create(url));
        this.playbackStarted = true;
    }
    @Override
    public boolean hasMedia() {
        return player != null && player.isPlaying();
    }
    @Override
    public void setRepeat(boolean value) {
        player.setRepeatMode(true);
    }
    @Override
    public long getTime() {
        return player.getTime();
    }
    @Override
    public void setTime(long value) {
        player.seekTo(value);
    }
    @Override
    public void preRender() {
        if (player instanceof VideoPlayer videoPlayer) {
            videoPlayer.preRender();
        }
    }
    @Override
    public int getTexture() {
        if (player instanceof VideoPlayer videoPlayer) {
            return videoPlayer.texture();
        }
        return -1;
    }
    @Override
    public boolean isReady() {
        return PlayerAPI.isReady();
    }
    @Override
    public boolean isWorking() {
        return mediaHandlers.containsKey(id) && mediaHandlers.get(id) instanceof WaterMediaHandler waterMediaHandler
                && waterMediaHandler.player != null && !waterMediaHandler.player.isBroken();
    }
}
