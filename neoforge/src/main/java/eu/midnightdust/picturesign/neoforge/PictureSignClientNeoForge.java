package eu.midnightdust.picturesign.neoforge;

import eu.midnightdust.picturesign.PictureSignClient;
import net.minecraft.resource.ResourcePackProfile;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

@SuppressWarnings("all")
public class PictureSignClientNeoForge {
    public static List<ResourcePackProfile> defaultEnabledPacks = Lists.newArrayList();

    public static void initClient() {
        PictureSignClient.init();
    }
    public static void doClientTick(ClientTickEvent.Pre event) {
    }
}