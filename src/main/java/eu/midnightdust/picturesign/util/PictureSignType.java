package eu.midnightdust.picturesign.util;

import net.minecraft.block.entity.SignBlockEntity;

public enum PictureSignType {
    NONE, PICTURE, VIDEO, LOOPED_VIDEO;
    public static PictureSignType getType(SignBlockEntity signBlockEntity, boolean front) {
        String rowOne = signBlockEntity.getText(front).getMessage(0,false).getString();
        if (rowOne.startsWith("!PS:")) return PICTURE;
        if (rowOne.startsWith("!VS:")) return VIDEO;
        if (rowOne.startsWith("!LS:")) return LOOPED_VIDEO;
        else return NONE;
    }
    public static boolean isType(SignBlockEntity signBlockEntity, PictureSignType type, boolean front) {
        return getType(signBlockEntity, front) == type;
    }
    public static boolean hasNoPicture(SignBlockEntity signBlockEntity) {
        return isType(signBlockEntity, NONE, true) && isType(signBlockEntity, NONE, false);
    }
}
