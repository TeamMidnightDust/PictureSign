package eu.midnightdust.picturesign.util;

import net.minecraft.block.entity.SignBlockEntity;

public class PictureURLUtils {
    public static String getLink(SignBlockEntity signBlockEntity) {
        String text = signBlockEntity.getTextOnRow(0, false).getString() +
                signBlockEntity.getTextOnRow(1, false).getString() +
                signBlockEntity.getTextOnRow(2, false).getString();
        String url = text.replaceAll("!PS:", "").replaceAll(" ","");
        if (url.startsWith("ps:")) url = url.replace("ps:", "https://pictshare.net/");
        if (url.startsWith("imgur:")) url = url.replace("imgur:", "https://i.imgur.com/");
        if (url.startsWith("imgbb:")) url = url.replace("imgbb:", "https://i.ibb.co/");
        if (url.startsWith("iili:")) url = url.replace("iili:", "https://iili.io/");
        if (url.startsWith("discord:")) url = url.replace("discord:", "https://cdn.discordapp.com/attachments/");
        return url;
    }
    public static String shortenLink(String url) {
        if (url.startsWith("https://")) {
            url = url.replace("https://", "");
        }
        if (url.startsWith("pictshare.net/")) url = url.replaceFirst("\\Qpictshare.net/\\E", "ps:");
        if (url.startsWith("i.imgur.com/")) url = url.replaceFirst("\\Qi.imgur.com/\\E", "imgur:");
        if (url.startsWith("i.ibb.co/:")) url = url.replaceFirst("\\Qi.ibb.co/\\E", "imgbb:");
        if (url.startsWith("iili.io/")) url = url.replaceFirst("\\Qiili.io/\\E", "iili:");
        if (url.startsWith("discord:")) url = url.replaceFirst("\\Qiili.io/\\E", "discord:");
        return url;
    }
}
