package eu.midnightdust.picturesign.config;

import eu.midnightdust.lib.config.MidnightConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PictureSignSecrets extends MidnightConfig {
    @Entry @Hidden
    public static Map<String, String> privateUrls = new HashMap<>();

    public static Optional<String> resolveSecret(String url) {
        if (url.endsWith("?api_key=REDACTED")) {
            return Optional.ofNullable(privateUrls.get(url));
        }
        return Optional.of(url);
    }

    public static String redactSecret(String url) {
        if (!PictureSignConfig.redactApiKeys) return url;

        if (url.matches(".*(\\?api_key=).*")) {
            String redactedURL = url.split("\\?api_key=")[0] + "?api_key=REDACTED";
            privateUrls.put(redactedURL, url);
            PictureSignSecrets.write("picturesign-secrets");
            return redactedURL;
        }
        return url;
    }
}
