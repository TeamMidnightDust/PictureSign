package eu.midnightdust.picturesign.neoforge;

import eu.midnightdust.picturesign.util.GIFHandler;
import eu.midnightdust.picturesign.util.MediaHandler;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import static eu.midnightdust.picturesign.PictureSignClient.id;
import static eu.midnightdust.picturesign.PictureSignClient.client;
import static eu.midnightdust.picturesign.PictureSignClient.clipboard;
import static eu.midnightdust.picturesign.PictureSignClient.MOD_ID;
import static eu.midnightdust.picturesign.PictureSignClient.BINDING_COPY_SIGN;

@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class PictureSignClientGameEvents {
    @SubscribeEvent()
    public static void sendPacketOnLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (MediaHandler.hasValidImplementation()) MediaHandler.closeAll();
    }
    @SubscribeEvent
    public static void onBlockEntityUnload(ChunkEvent.Unload event) {
        if (MediaHandler.hasValidImplementation()) {
            for (BlockPos pos : event.getChunk().getBlockEntityPositions()) {
                Identifier videoId = id(pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + "_f");
                MediaHandler.closePlayer(videoId);
                Identifier videoId2 = id(pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + "_b");
                MediaHandler.closePlayer(videoId2);
            }
        }
    }
    @SubscribeEvent
    public static void endClientTick(ClientTickEvent.Post event) {
        GIFHandler.gifPlayers.forEach(((identifier, handler) -> handler.tick()));
        if (!BINDING_COPY_SIGN.isPressed()) return;
        BINDING_COPY_SIGN.setPressed(false);
        if (client.player == null || client.world == null || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        if (client.crosshairTarget.getType() == HitResult.Type.BLOCK && client.world.getBlockState(BlockPos.ofFloored(client.crosshairTarget.getPos())).hasBlockEntity()) {
            if (client.world.getBlockEntity(BlockPos.ofFloored(client.crosshairTarget.getPos())) instanceof SignBlockEntity sign) {
                boolean front = sign.isPlayerFacingFront(client.player);
                for (int i = 0; i < 4; i++) {
                    clipboard[i] = sign.getText(front).getMessage(i, false).getString();
                }
            }
        }
    }
}
