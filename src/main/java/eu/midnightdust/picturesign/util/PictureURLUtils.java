package eu.midnightdust.picturesign.util;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import net.minecraft.block.entity.SignBlockEntity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
public class PictureURLUtils {
    public static final Type STRING_TYPE = new TypeToken<Map<String, String>>(){}.getType();
    public static final Map<String, PictureInfo> cachedJsonData = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().create();
    public static PictureInfo infoFromJson(String pathToJson) {
        if (cachedJsonData.containsKey(pathToJson)) return cachedJsonData.get(pathToJson);
        PictureInfo result = null;
        URL json = toURL(pathToJson);
        Map<String, String> jsonData = null;

        try (Reader reader = new InputStreamReader(json.openStream())) {
            jsonData = GSON.fromJson(reader, STRING_TYPE);
        } catch (MalformedURLException error) {
            PictureSignClient.LOGGER.error("Unable to load url from JSON because of connection problems: " + error.getMessage());
        } catch (IOException error) {
            PictureSignClient.LOGGER.error("Unable to load url from JSON because of an I/O Exception: " + error.getMessage());
        }
        if (PictureSignConfig.debug) PictureSignClient.LOGGER.info("JsonData: "+jsonData);
        if (jsonData != null && !jsonData.isEmpty() && jsonData.containsKey("url")) {
            result = new PictureInfo(jsonData.get("url"), getDurationMillis(jsonData.getOrDefault("start_at", "")),
                    getDurationMillis(jsonData.getOrDefault("end_at", "")), Integer.parseInt(jsonData.getOrDefault("volume", "-1")));
            PictureSignClient.LOGGER.info("URL successfully loaded from JSON!");
        } else {
            PictureSignClient.LOGGER.warn("Unable to load url from JSON");
        }
        if (PictureSignConfig.debug) PictureSignClient.LOGGER.info("Result: "+result);
        cachedJsonData.put(pathToJson, result);
        return result;
    }
    private static long getDurationMillis(String duration) {
        if (duration.equals("")) return -1;
        String[] splitDuration = duration.split(":");
        if (splitDuration.length != 4) return -1;
        return TimeUnit.MILLISECONDS.convert(Duration.parse("PT" + splitDuration[0]+"H" + splitDuration[1]+"M" + splitDuration[2]+"S")) + Long.parseLong(splitDuration[3]);
    }
    public static URL toURL(String string) {
        URL result = null;
        try { result = new URL(string); }
        catch (MalformedURLException e) {PictureSignClient.LOGGER.warn("Malformed URL: " + e);}
        return result;
    }
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
