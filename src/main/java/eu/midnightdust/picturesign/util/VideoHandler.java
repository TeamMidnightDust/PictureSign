package eu.midnightdust.picturesign.util;

import com.igrium.videolib.VideoLib;
import com.igrium.videolib.api.VideoManager;
import net.minecraft.util.Identifier;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class VideoHandler {
    public static List<Identifier> videoPlayers = new ArrayList<>();
    public static List<Identifier> playedOnce = new ArrayList<>();
    static VideoManager videoManager = VideoLib.getInstance().getVideoManager();
    public static void closePlayer(Identifier id) {
        videoManager.closePlayer(id);
        videoPlayers.remove(id);
        playedOnce.remove(id);
    }
    public static void stop(Identifier id) {
        videoManager.getOrCreate(id).getControlsInterface().stop();
    }
    public static void play(Identifier id, String url) throws MalformedURLException {
        videoManager.getOrCreate(id).getMediaInterface().play(url);
    }
    public static boolean hasMedia(Identifier id) {
        return videoManager.getOrCreate(id).getMediaInterface().hasMedia();
    }
    public static void setRepeat(Identifier id, boolean value) {
        videoManager.getOrCreate(id).getControlsInterface().setRepeat(value);
    }
    public static long getTime(Identifier id) {
        return videoManager.getOrCreate(id).getControlsInterface().getTime();
    }
    public static void setTime(Identifier id, long value) {
        videoManager.getOrCreate(id).getControlsInterface().setTime(value);
    }
    public static Identifier getTexture(Identifier id) {
        return videoManager.getOrCreate(id).getTexture();
    }
    public static float getFramerate(Identifier id) {
        return videoManager.getOrCreate(id).getCodecInterface().getFrameRate();
    }
}
