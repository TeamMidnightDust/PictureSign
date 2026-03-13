package eu.midnightdust.picturesign.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public enum PictureSignType {
    NONE(Component.empty(), "", false, false, false),
    PICTURE(Component.translatable("picturesign.type.image"), "!PS:", false, false, false),
    GIF(Component.translatable("picturesign.type.gif"), "!GS:", true, false, false),
    VIDEO(Component.translatable("picturesign.type.video"), "!VS:", false, true, false),
    LOOPED_VIDEO(Component.translatable("picturesign.type.video_loop"), "!LS:", true, true, false),
    AUDIO(Component.translatable("picturesign.type.audio"), "!AS:", false, false, true),
    LOOPED_AUDIO(Component.translatable("picturesign.type.audio_loop"), "!LAS:", true, false, true);

    public final Component name;
    public final String format;
    public final boolean isLooped;
    public final boolean isVideo;
    public final boolean isAudio;

    PictureSignType(Component name, String format, boolean isLooped, boolean isVideo, boolean isAudio) {
        this.name = name;
        this.format = format;
        this.isLooped = isLooped;
        this.isVideo = isVideo;
        this.isAudio = isAudio;
    }

    public static PictureSignType getType(SignBlockEntity signBlockEntity, boolean front) {
        if (signBlockEntity == null) return PictureSignType.NONE;
        return getType(signBlockEntity.getText(front).getMessage(0,false).getString());
    }
    public static PictureSignType getType(String lineOne) {
        if (lineOne.startsWith("!PS:")) return PICTURE;
        else if (lineOne.startsWith("!GS:")) return GIF;
        else if (lineOne.startsWith("!VS:")) return VIDEO;
        else if (lineOne.startsWith("!LS:")) return LOOPED_VIDEO;
        else if (lineOne.startsWith("!AS:")) return AUDIO;
        else if (lineOne.startsWith("!LAS:")) return LOOPED_AUDIO;
        else return NONE;
    }
    public PictureSignType next() {
        return switch (this) {
            case PICTURE -> GIF;
            case GIF -> VIDEO;
            case VIDEO -> LOOPED_VIDEO;
            case LOOPED_VIDEO -> AUDIO;
            case AUDIO -> LOOPED_AUDIO;
            case LOOPED_AUDIO -> PICTURE;
            default -> NONE;
        };
    }

    public static boolean isCandidate(SignBlockEntity signBlockEntity, boolean front) {
        return signBlockEntity.getText(front).getMessage(0,false).getString().startsWith("!");
    }
    public static boolean isNotOfType(SignBlockEntity signBlockEntity, PictureSignType type, boolean front) {
        return getType(signBlockEntity, front) != type;
    }
    public static boolean hasPicture(SignBlockEntity signBlockEntity) {
        return isNotOfType(signBlockEntity, NONE, true) || isNotOfType(signBlockEntity, NONE, false);
    }
}
