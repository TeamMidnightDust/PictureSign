package eu.midnightdust.picturesign.fabric;

import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.util.GIFHandler;
import eu.midnightdust.picturesign.util.MediaHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import static eu.midnightdust.picturesign.PictureSignClient.BINDING_COPY_SIGN;
import static eu.midnightdust.picturesign.PictureSignClient.id;
import static eu.midnightdust.picturesign.PictureSignClient.clipboard;

public class PictureSignClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PictureSignClient.init();

        KeyBindingHelper.registerKeyBinding(BINDING_COPY_SIGN);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (MediaHandler.hasValidImplementation()) MediaHandler.closeAll();
        });
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, world) -> {
            if (MediaHandler.hasValidImplementation()) {
                BlockPos pos = blockEntity.getPos();
                Identifier videoId = id(pos.getX() + "_" + pos.getY() + "_" + pos.getZ()+"_f");
                MediaHandler.closePlayer(videoId);
                Identifier videoId2 = id(pos.getX() + "_" + pos.getY() + "_" + pos.getZ()+"_b");
                MediaHandler.closePlayer(videoId2);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            GIFHandler.gifHandlers.forEach(((identifier, handler) -> handler.tick()));
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
        });
    }
}
