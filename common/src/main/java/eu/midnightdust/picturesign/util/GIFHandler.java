package eu.midnightdust.picturesign.util;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class GIFHandler {
    private static final List<Function<Identifier, GIFHandler>> implementations = new ArrayList<>();
    public static Map<Identifier, GIFHandler> gifHandlers = new HashMap<>();

    public final Identifier id;
    public boolean playbackStarted;

    public GIFHandler(Identifier id) {
        this.id = id;
        gifHandlers.put(id, this);
    }
    public static void registerHandler(Function<Identifier, GIFHandler> handler) {
        implementations.add(handler);
    }
    public static boolean hasValidImplementation() {
        return !implementations.isEmpty();
    }
    public static GIFHandler getOrCreate(Identifier id) {
        if (gifHandlers.containsKey(id)) return gifHandlers.get(id);
        AtomicReference<GIFHandler> handler = new AtomicReference<>();
        implementations.forEach(impl -> {
            handler.set(impl.apply(id));
        });
        return handler.get();
    }
    public void tick() {
    }

    public void closePlayer() {
        gifHandlers.remove(this.id);
    }
    public static void closePlayer(Identifier videoId) {
        if (gifHandlers.containsKey(videoId)) gifHandlers.get(videoId).closePlayer();
    }
    public static void closeAll() {
        gifHandlers.forEach((id, handler) -> handler.closePlayer());
        gifHandlers.clear();
    }

    public void play(String url) {
        this.playbackStarted = true;
    }
    public boolean hasMedia() {
        return false;
    }
    public int getTexture() {
        return -1;
    }
    public boolean isWorking() {
        return false;
    }
}
