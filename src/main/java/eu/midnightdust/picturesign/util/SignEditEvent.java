package eu.midnightdust.picturesign.util;

import eu.midnightdust.picturesign.PictureSignClient;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

public class SignEditEvent {
    public static void tick(@NotNull MinecraftClient client) {
        if (!PictureSignClient.BINDING_EDIT_SIGN.isPressed()) return;
        PictureSignClient.BINDING_EDIT_SIGN.setPressed(false);
        if (client.player == null || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        if (client.crosshairTarget.getType() == HitResult.Type.BLOCK && client.world.getBlockState(new BlockPos(client.crosshairTarget.getPos())).hasBlockEntity()) {
            if (client.world.getBlockEntity(new BlockPos(client.crosshairTarget.getPos())) instanceof SignBlockEntity) {
                client.setScreen(new SignEditScreen((SignBlockEntity) client.world.getBlockEntity(new BlockPos(client.crosshairTarget.getPos())), false));
            }
        }
    }
}
