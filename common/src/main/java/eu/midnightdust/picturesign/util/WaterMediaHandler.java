package eu.midnightdust.picturesign.util;

import me.srrapero720.watermedia.api.player.SyncBasePlayer;
import me.srrapero720.watermedia.api.player.SyncMusicPlayer;
import me.srrapero720.watermedia.api.player.SyncVideoPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class WaterMediaHandler extends MediaHandler {
    private SyncBasePlayer player;

    WaterMediaHandler(Identifier id, BlockPos pos) {
        super(id, pos);
        mediaHandlers.put(id, this);
    }
    @Override
    void setVolume(int volume) {
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
        isDeactivated = true;
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
        this.player = isVideo ? new SyncVideoPlayer(MinecraftClient.getInstance()) : new SyncMusicPlayer();
        mediaHandlers.put(id, this);
        if (player.isBroken()) return;
        player.start(url);
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
    public int getTexture() {
        if (player instanceof SyncVideoPlayer videoPlayer) return videoPlayer.getGlTexture();
        return -1;
    }
    @Override
    public boolean isWorking() {
        return mediaHandlers.containsKey(id) && mediaHandlers.get(id) instanceof WaterMediaHandler waterMediaHandler
                && waterMediaHandler.player != null && !waterMediaHandler.player.isBroken();
    }
}
