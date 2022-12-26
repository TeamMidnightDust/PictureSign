package eu.midnightdust.picturesign.mixin;

import com.igrium.videolib.VideoLib;
import eu.midnightdust.picturesign.render.PictureSignRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = SignBlockEntity.class, priority = 1100)
public abstract class MixinSignBlockEntity extends BlockEntity {
    public MixinSignBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    @Unique
    public void markRemoved() {
        VideoLib.getInstance().getVideoManager().closePlayer(new Identifier("picturesign", pos.getX() + "." + pos.getY() + "." + pos.getZ()));
        PictureSignRenderer.videoPlayers.remove(new Identifier("picturesign", pos.getX() + "." + pos.getY() + "." + pos.getZ()));
        super.markRemoved();
    }
}
