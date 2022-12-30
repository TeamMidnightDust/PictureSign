package eu.midnightdust.picturesign.util;

import net.minecraft.block.entity.SignBlockEntity;

public enum PictureSignType {
    NONE, PICTURE, VIDEO, LOOPED_VIDEO;
    public static PictureSignType getType(SignBlockEntity signBlockEntity) {
        String rowOne = signBlockEntity.getTextOnRow(0,false).getString();
        if (rowOne.startsWith("!PS:")) return PICTURE;
        if (rowOne.startsWith("!VS:")) return VIDEO;
        if (rowOne.startsWith("!LS:")) return LOOPED_VIDEO;
        else return NONE;
    }
    public static boolean isType(SignBlockEntity signBlockEntity, PictureSignType type) {
        return getType(signBlockEntity) == type;
    }
}
