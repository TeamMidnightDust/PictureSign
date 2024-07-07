package eu.midnightdust.picturesign.neoforge;

import eu.midnightdust.picturesign.PictureSignClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

import static eu.midnightdust.picturesign.PictureSignClient.MOD_ID;

@SuppressWarnings("all")
@Mod(value = MOD_ID, dist = Dist.CLIENT)
public class PictureSignClientNeoForge {
    public PictureSignClientNeoForge() {
        PictureSignClient.init();
    }
}