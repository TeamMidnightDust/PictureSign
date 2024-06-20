package eu.midnightdust.picturesign.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.packet.Packet;

public class NetworkUtil {
    @ExpectPlatform
    public static void sendPacket(Packet<?> packet) {
        throw new AssertionError();
    }
}
