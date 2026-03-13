package eu.midnightdust.picturesign.media;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

public abstract class GIFHandler {
    private static final List<Function<ResourceLocation, GIFHandler>> implementations = new ArrayList<>();
    public static Map<ResourceLocation, GIFHandler> gifHandlers = new HashMap<>();

    public final ResourceLocation id;
    public boolean playbackStarted;

    public GIFHandler(ResourceLocation id) {
        this.id = id;
        gifHandlers.put(id, this);
    }

    public static void registerHandler(Function<ResourceLocation, GIFHandler> handler) {
        implementations.add(handler);
    }

    public static boolean hasValidImplementation() {
        return !implementations.isEmpty();
    }

    public static GIFHandler getOrCreate(ResourceLocation id) {
        if (gifHandlers.containsKey(id)) return gifHandlers.get(id);
        AtomicReference<GIFHandler> handler = new AtomicReference<>();
        implementations.forEach(impl -> {
            handler.set(impl.apply(id));
        });
        return handler.get();
    }

    public static void closePlayer(ResourceLocation videoId) {
        if (gifHandlers.containsKey(videoId)) gifHandlers.get(videoId).closePlayer();
    }

    public static void closeAll() {
        gifHandlers.forEach((id, handler) -> handler.closePlayer());
        gifHandlers.clear();
    }

    public void play(String url) {
        this.playbackStarted = true;
    }

    public void tick() {
        // Optional
    }

    public void closePlayer() {
        gifHandlers.remove(this.id);
    }

    public abstract boolean hasMedia();

    public abstract int getTexture();

    public abstract boolean isWorking();
}
