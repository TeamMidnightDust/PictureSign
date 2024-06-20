package eu.midnightdust.picturesign.neoforge;

import eu.midnightdust.picturesign.PictureSignClient;
import net.minecraft.resource.ResourcePackProfile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

import static eu.midnightdust.picturesign.PictureSignClient.MOD_ID;

@SuppressWarnings("all")
@Mod(value = MOD_ID, dist = Dist.CLIENT)
public class PictureSignClientNeoForge {
    public static List<ResourcePackProfile> defaultEnabledPacks = Lists.newArrayList();

    public PictureSignClientNeoForge() {
        PictureSignClient.init();
    }
}