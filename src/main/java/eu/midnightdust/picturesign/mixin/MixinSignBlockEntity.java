package eu.midnightdust.picturesign.mixin;

import eu.midnightdust.picturesign.util.VideoHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import static eu.midnightdust.picturesign.PictureSignClient.MOD_ID;

@Mixin(value = SignBlockEntity.class, priority = 1100)
public abstract class MixinSignBlockEntity extends BlockEntity {
    public MixinSignBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    @Unique
    public void markRemoved() {
        Identifier videoId = new Identifier(MOD_ID, pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + "_f");
        Identifier videoId2 = new Identifier(MOD_ID, pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + "_b");
        VideoHandler.closePlayer(videoId);
        VideoHandler.closePlayer(videoId2);
        super.markRemoved();
    }
}
