package eu.midnightdust.picturesign.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import static eu.midnightdust.picturesign.PictureSignClient.MOD_ID;
import static eu.midnightdust.picturesign.PictureSignClient.BINDING_COPY_SIGN;

@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PictureSignClientEvents {
    @SubscribeEvent
    public static void registerKeybinding(RegisterKeyMappingsEvent event) {
        event.register(BINDING_COPY_SIGN);
    }
}
