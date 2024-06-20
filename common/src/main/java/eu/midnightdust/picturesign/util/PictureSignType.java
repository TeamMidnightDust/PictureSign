package eu.midnightdust.picturesign.util;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.text.Text;

public enum PictureSignType {
    NONE(Text.empty(),                        ""),
    PICTURE(Text.of("Image"),           "!PS:"),
    VIDEO(Text.of("Video"),             "!VS:", false, true, false),
    LOOPED_VIDEO(Text.of("Video Loop"), "!LS:", true, true, false),
    AUDIO(Text.of("Audio"),             "!AS:", false, false, true),
    LOOPED_AUDIO(Text.of("Audio Loop"), "!LAS:", true, false, true);

    public final Text name;
    public final String format;
    public final boolean isLooped;
    public final boolean isVideo;
    public final boolean isAudio;
    PictureSignType(Text name, String format) {
        this(name, format, false, false, false);
    }

    PictureSignType(Text name, String format, boolean isLooped, boolean isVideo, boolean isAudio) {
        this.name = name;
        this.format = format;
        this.isLooped = isLooped;
        this.isVideo = isVideo;
        this.isAudio = isAudio;
    }

    public static PictureSignType getType(SignBlockEntity signBlockEntity, boolean front) {
        return getType(signBlockEntity.getText(front).getMessage(0,false).getString());
    }
    public static PictureSignType getType(String lineOne) {
        if (lineOne.startsWith("!PS:")) return PICTURE;
        else if (lineOne.startsWith("!VS:")) return VIDEO;
        else if (lineOne.startsWith("!LS:")) return LOOPED_VIDEO;
        else if (lineOne.startsWith("!AS:")) return AUDIO;
        else if (lineOne.startsWith("!LAS:")) return LOOPED_AUDIO;
        else return NONE;
    }
    public PictureSignType next() {
        return switch (this) {
            case PICTURE -> VIDEO;
            case VIDEO -> LOOPED_VIDEO;
            case LOOPED_VIDEO -> AUDIO;
            case AUDIO -> LOOPED_AUDIO;
            case LOOPED_AUDIO -> PICTURE;
            default -> NONE;
        };
    }

    public static boolean isNotOfType(SignBlockEntity signBlockEntity, PictureSignType type, boolean front) {
        return getType(signBlockEntity, front) != type;
    }
    public static boolean hasPicture(SignBlockEntity signBlockEntity) {
        return isNotOfType(signBlockEntity, NONE, true) || isNotOfType(signBlockEntity, NONE, false);
    }
}
