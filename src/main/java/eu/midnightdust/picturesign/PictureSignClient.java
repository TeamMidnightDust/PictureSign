package eu.midnightdust.picturesign;

import eu.midnightdust.picturesign.config.PictureSignConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

public class PictureSignClient implements ClientModInitializer {
    public static Logger LOGGER = LogManager.getLogger("PictureSign");
    public static String[] clipboard = new String[4];
    public static final KeyBinding BINDING_EDIT_SIGN = new KeyBinding("key.picturesign.edit_sign",
            InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.categories.picturesign");
    public static final KeyBinding BINDING_COPY_SIGN = new KeyBinding("key.picturesign.copy_sign",
            InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, "key.categories.picturesign");
    @Override
    public void onInitializeClient() {
        PictureSignConfig.init("picturesign", PictureSignConfig.class);

        KeyBindingHelper.registerKeyBinding(BINDING_COPY_SIGN);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!PictureSignClient.BINDING_COPY_SIGN.isPressed()) return;
            PictureSignClient.BINDING_COPY_SIGN.setPressed(false);
            if (client.player == null || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
            if (client.crosshairTarget.getType() == HitResult.Type.BLOCK && client.world.getBlockState(new BlockPos(client.crosshairTarget.getPos())).hasBlockEntity()) {
                if (client.world.getBlockEntity(new BlockPos(client.crosshairTarget.getPos())) instanceof SignBlockEntity) {
                    SignBlockEntity sign = (SignBlockEntity) client.world.getBlockEntity(new BlockPos(client.crosshairTarget.getPos()));
                    clipboard[0] = sign.getTextOnRow(0, false).asString();
                    clipboard[1] = sign.getTextOnRow(1, false).asString();
                    clipboard[2] = sign.getTextOnRow(2, false).asString();
                    clipboard[3] = sign.getTextOnRow(3, false).asString();
                }
            }
        });
        if (PictureSignConfig.debug) {
            KeyBindingHelper.registerKeyBinding(BINDING_EDIT_SIGN);
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (!PictureSignClient.BINDING_EDIT_SIGN.isPressed()) return;
                PictureSignClient.BINDING_EDIT_SIGN.setPressed(false);
                if (client.player == null || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
                if (client.crosshairTarget.getType() == HitResult.Type.BLOCK && client.world.getBlockState(new BlockPos(client.crosshairTarget.getPos())).hasBlockEntity()) {
                    if (client.world.getBlockEntity(new BlockPos(client.crosshairTarget.getPos())) instanceof SignBlockEntity) {
                        BlockPos pos = new BlockPos(client.crosshairTarget.getPos());
                        SignBlockEntity sign = (SignBlockEntity) client.world.getBlockEntity(pos);
                        sign.setEditable(true);
                        sign.setEditor(client.player.getUuid());
                        sign.toUpdatePacket();
                        sign.markDirty();
                        client.world.updateListeners(pos, sign.getCachedState(), sign.getCachedState(), 3);
                        client.setScreen(new SignEditScreen(sign,false));
//                    clipboard[0] = sign.getTextOnRow(0, false).asString();
//                    clipboard[1] = sign.getTextOnRow(1, false).asString();
//                    clipboard[2] = sign.getTextOnRow(2, false).asString();
//                    clipboard[3] = sign.getTextOnRow(3, false).asString();
//                    Block signBlock = sign.getCachedState().getBlock();
//                    client.interactionManager.breakBlock(pos);
//                    client.player.setStackInHand(client.player.preferredHand, new ItemStack(signBlock.asItem()));
//                    client.interactionManager.interactBlock(client.player, client.world, client.player.preferredHand, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.DOWN, pos,false));
                    }
                }
            });
        }
    }
}
