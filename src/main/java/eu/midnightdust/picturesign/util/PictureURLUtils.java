package eu.midnightdust.picturesign.util;

import net.minecraft.block.entity.SignBlockEntity;

public class PictureURLUtils {
    public static String getLink(SignBlockEntity signBlockEntity) {
        String text = signBlockEntity.getTextOnRow(0, false).getString() +
                signBlockEntity.getTextOnRow(1, false).getString() +
                signBlockEntity.getTextOnRow(2, false).getString();
        String url = text.replaceAll("!PS:", "").replaceAll("!VS:", "").replaceAll("!LS:", "").replaceAll(" ","");
        if (url.startsWith("ps:")) url = url.replace("ps:", "https://pictshare.net/");
        if (url.startsWith("imgur:")) url = url.replace("imgur:", "https://i.imgur.com/");
        if (url.startsWith("imgbb:")) url = url.replace("imgbb:", "https://i.ibb.co/");
        if (url.startsWith("iili:")) url = url.replace("iili:", "https://iili.io/");
        return url;
    }
    public static String shortenLink(String url) {
        if (url.contains("pictshare.net/")) url = url.replace("pictshare.net/", "ps:");
        if (url.contains("i.imgur.com/")) url = url.replace("i.imgur.com/", "imgur:");
        if (url.contains("i.ibb.co/:")) url = url.replace("i.ibb.co/", "imgbb:");
        if (url.contains("iili.io/")) url = url.replace("iili.io/", "iili:");
        if (url.startsWith("https://")) {
            url = url.replace("https://", "");
        }
        return url;
    }
}
