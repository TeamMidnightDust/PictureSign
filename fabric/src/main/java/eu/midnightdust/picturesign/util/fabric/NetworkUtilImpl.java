package eu.midnightdust.picturesign.util.fabric;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;

import static eu.midnightdust.picturesign.PictureSignClient.client;

public class NetworkUtilImpl {
    private static final ClientPlayNetworkHandler handler = client.getNetworkHandler();

    public static void sendPacket(Packet<?> packet) {
        if (handler != null)
            handler.sendPacket(packet);
    }
}
