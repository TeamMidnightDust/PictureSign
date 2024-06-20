package eu.midnightdust.picturesign.mixin;

import eu.midnightdust.picturesign.util.MediaHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import static eu.midnightdust.picturesign.PictureSignClient.id;

@Mixin(value = SignBlockEntity.class, priority = 1100)
public abstract class MixinSignBlockEntity extends BlockEntity {
    public MixinSignBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    @Unique
    public void markRemoved() {
        Identifier videoId = id(pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + "_f");
        Identifier videoId2 = id(pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + "_b");
        MediaHandler.closePlayer(videoId);
        MediaHandler.closePlayer(videoId2);
        super.markRemoved();
    }
}
