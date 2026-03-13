package eu.midnightdust.picturesign;

import com.mojang.blaze3d.platform.InputConstants;
import eu.midnightdust.lib.util.PlatformFunctions;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.config.PictureSignSecrets;
import eu.midnightdust.picturesign.media.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.HitResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
//?} else if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
*///?}

public abstract class PictureSignClient {
    public static Logger LOGGER = LogManager.getLogger("PictureSign");
    public static final String MOD_ID = "picturesign";
    public static String[] clipboard = new String[4];
    public static final KeyMapping BINDING_COPY_SIGN = new KeyMapping("key.%s.copy_sign".formatted(MOD_ID),
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, "key.categories.%s".formatted(MOD_ID));

    public static void init() {
        PictureSignConfig.init(MOD_ID, PictureSignConfig.class);
        PictureSignSecrets.init("picturesign-secrets", PictureSignSecrets.class);
        if (PlatformFunctions.isModLoaded("watermedia")) {
            WaterMediaCompat.init();
        }
    }
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void onEndClientTick(Minecraft client) {
        GIFHandler.gifHandlers.forEach(((identifier, handler) -> handler.tick()));
        if (!BINDING_COPY_SIGN.isDown()) return;
        BINDING_COPY_SIGN.setDown(false);
        if (client.player == null || client.level == null || client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) return;
        if (client.hitResult.getType() == HitResult.Type.BLOCK && client.level.getBlockState(BlockPos.containing(client.hitResult.getLocation())).hasBlockEntity()) {
            if (client.level.getBlockEntity(BlockPos.containing(client.hitResult.getLocation())) instanceof SignBlockEntity sign) {
                boolean front = sign.isFacingFrontText(client.player);
                for (int i = 0; i < 4; i++) {
                    clipboard[i] = sign.getText(front).getMessage(i, false).getString();
                }
            }
        }
    }

    private static void onBlockEntityUnload(BlockPos pos) {
        if (MediaHandler.hasValidImplementation()) {
            ResourceLocation videoId = id(pos.getX() + "_" + pos.getY() + "_" + pos.getZ()+"_f");
            MediaHandler.closePlayer(videoId);
            ResourceLocation videoId2 = id(pos.getX() + "_" + pos.getY() + "_" + pos.getZ()+"_b");
            MediaHandler.closePlayer(videoId2);
        }
    }

    //? fabric {
    public static class PictureSignClientFabric implements ClientModInitializer {
        @Override
        public void onInitializeClient() {
            PictureSignClient.init();

            KeyBindingHelper.registerKeyBinding(BINDING_COPY_SIGN);
            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
                if (MediaHandler.hasValidImplementation()) MediaHandler.closeAll();
            });
            ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, w) -> PictureSignClient.onBlockEntityUnload(be.getBlockPos()));
            ClientTickEvents.END_CLIENT_TICK.register(PictureSignClient::onEndClientTick);
        }
    }
    //?}

    //? neoforge {
    /*@SuppressWarnings("all")
    @Mod(value = MOD_ID, dist = Dist.CLIENT)
    public static class PictureSignClientNeoForge {
        public PictureSignClientNeoForge() {
            PictureSignClient.init();
        }
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class PictureSignClientEvents {
        @SubscribeEvent
        public static void registerKeybinding(RegisterKeyMappingsEvent event) {
            event.register(BINDING_COPY_SIGN);
        }
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class PictureSignClientGameEvents {
        @SubscribeEvent()
        public static void sendPacketOnLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            if (MediaHandler.hasValidImplementation()) MediaHandler.closeAll();
        }
        @SubscribeEvent
        public static void onBlockEntityUnload(ChunkEvent.Unload event) {
            if (MediaHandler.hasValidImplementation()) {
                for (BlockPos pos : event.getChunk().getBlockEntitiesPos()) {
                    PictureSignClient.onBlockEntityUnload(pos);
                }
            }
        }
        @SubscribeEvent
        public static void endClientTick(ClientTickEvent.Post event) {
            PictureSignClient.onEndClientTick(Minecraft.getInstance());
        }
    }
    *///?}
}
