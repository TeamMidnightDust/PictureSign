package eu.midnightdust.picturesign.mixin;

import eu.midnightdust.picturesign.media.MediaHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    public void setRemoved() {
        ResourceLocation videoId = id(worldPosition.getX() + "_" + worldPosition.getY() + "_" + worldPosition.getZ() + "_f");
        ResourceLocation videoId2 = id(worldPosition.getX() + "_" + worldPosition.getY() + "_" + worldPosition.getZ() + "_b");
        MediaHandler.closePlayer(videoId);
        MediaHandler.closePlayer(videoId2);
        super.setRemoved();
    }
}
