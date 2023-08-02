package eu.midnightdust.picturesign.util;

import net.minecraft.util.Identifier;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import eu.midnightdust.picturesign.ogv.VideoManager;

public class VideoHandler {
    public static List<Identifier> videoPlayers = new ArrayList<>();
    public static List<Identifier> playedOnce = new ArrayList<>();
    public static VideoManager videoManager = new VideoManager();
    public static void closePlayer(Identifier id) {
        videoManager.closePlayer(id);
        videoPlayers.remove(id);
        playedOnce.remove(id);
    }
    public static void stop(Identifier id) {
        videoManager.getOrCreate(id).stop();
    }
    public static void play(Identifier id, String url) throws MalformedURLException {
        videoManager.getOrCreate(id).play(url);
    }
    public static boolean hasMedia(Identifier id) {
        return videoManager.getOrCreate(id).hasMedia();
    }
    public static void setRepeat(Identifier id, boolean value) {
        videoManager.getOrCreate(id).setRepeat(value);
    }
    public static long getTime(Identifier id) {
        return videoManager.getOrCreate(id).getTime();
    }
    public static void setTime(Identifier id, long value) {
        videoManager.getOrCreate(id).setTime(value);
    }
    public static Identifier getTexture(Identifier id) {
        return videoManager.getOrCreate(id).getTexture();
    }
    public static float getFramerate(Identifier id) {
        return videoManager.getOrCreate(id).getFrameRate();
    }
}