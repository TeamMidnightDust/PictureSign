package eu.midnightdust.picturesign;

import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.util.VideoHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

public class PictureSignClient implements ClientModInitializer {
    public static Logger LOGGER = LogManager.getLogger("PictureSign");
    public static String MOD_ID = "picturesign";
    public static String[] clipboard = new String[4];
    public static final KeyBinding BINDING_COPY_SIGN = new KeyBinding("key."+MOD_ID+".copy_sign",
            InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, "key.categories."+MOD_ID);
    @Override
    public void onInitializeClient() {
        PictureSignConfig.init(MOD_ID, PictureSignConfig.class);

        KeyBindingHelper.registerKeyBinding(BINDING_COPY_SIGN);
        ClientLoginConnectionEvents.DISCONNECT.register((handler, client) -> {
            /*if (PlatformFunctions.isModLoaded("videolib"))*/ {
                VideoHandler.videoPlayers.forEach(VideoHandler::closePlayer);
                VideoHandler.playedOnce.clear();
            }
        });
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, world) -> {
            /*if (PlatformFunctions.isModLoaded("videolib"))*/ {
                BlockPos pos = blockEntity.getPos();
                Identifier videoId = new Identifier(MOD_ID, pos.getX() + "_" + pos.getY() + "_" + pos.getZ()+"_f");
                VideoHandler.closePlayer(videoId);
                Identifier videoId2 = new Identifier(MOD_ID, pos.getX() + "_" + pos.getY() + "_" + pos.getZ()+"_b");
                VideoHandler.closePlayer(videoId2);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!PictureSignClient.BINDING_COPY_SIGN.isPressed()) return;
            PictureSignClient.BINDING_COPY_SIGN.setPressed(false);
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
