package eu.midnightdust.picturesign.media;
//
//import org.watermedia.api.player.PlayerAPI;
//import org.watermedia.api.player.videolan.BasePlayer;
//import org.watermedia.api.player.videolan.MusicPlayer;
//import org.watermedia.api.player.videolan.VideoPlayer;
//
//import java.net.URI;
//import net.minecraft.client.Minecraft;
//import net.minecraft.core.BlockPos;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.sounds.SoundSource;
//
//import static eu.midnightdust.picturesign.PictureSignClient.client;
//
//public class WM2MediaHandler extends MediaHandler {
//    private BasePlayer player;
//
//    public WM2MediaHandler(ResourceLocation id, BlockPos pos) {
//        super(id, pos);
//        mediaHandlers.put(id, this);
//    }
//    @Override
//    public void setVolume(int volume) {
//        player.setVolume((int) (volume * Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER)));
//    }
//
//    @Override
//    public void closePlayer() {
//        if (player != null) {
//            player.stop();
//            player.release();
//        }
//        //mediaPlayers.remove(id);
//        player = null;
//    }
//    @Override
//    public void stop() {
//        player.stop();
//        super.stop();
//    }
//    @Override
//    public boolean isStopped() {
//        return player.isStopped();
//    }
//    @Override
//    public boolean isPaused() {
//        return player.isPaused();
//    }
//    @Override
//    public void pause() {
//        player.pause();
//    }
//    @Override
//    public void restart() {
//        player.play();
//    }
//
//    @Override
//    public void play(String url, boolean isVideo) {
//        this.player = isVideo ? new VideoPlayer(client) : new MusicPlayer();
//        mediaHandlers.put(id, this);
//        if (player.isBroken()) return;
//        player.start(URI.create(url));
//        this.playbackStarted = true;
//    }
//    @Override
//    public boolean hasMedia() {
//        return player != null && player.isPlaying();
//    }
//    @Override
//    public void setRepeat(boolean value) {
//        player.setRepeatMode(true);
//    }
//    @Override
//    public long getTime() {
//        return player.getTime();
//    }
//    @Override
//    public void setTime(long value) {
//        player.seekTo(value);
//    }
//    @Override
//    public int getTexture() {
//        if (player instanceof VideoPlayer videoPlayer) {
//            videoPlayer.preRender();
//            return videoPlayer.texture();
//        }
//        return -1;
//    }
//    @Override
//    public boolean isReady() {
//        return PlayerAPI.isReady();
//    }
//    @Override
//    public boolean isWorking() {
//        return mediaHandlers.containsKey(id) && mediaHandlers.get(id) instanceof WM2MediaHandler WM2MediaHandler
//                && WM2MediaHandler.player != null && !WM2MediaHandler.player.isBroken();
//    }
//}
