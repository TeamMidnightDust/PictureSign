package eu.midnightdust.picturesign.util;

import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;

import static eu.midnightdust.picturesign.PictureSignClient.MOD_ID;
import static java.util.concurrent.Executors.newFixedThreadPool;

public class PictureDownloader {

    public static class PictureData {
        public String url;
        public Identifier identifier;

        public PictureData(String url) {
            this.url = url;
        }
    }
    static PictureDownloader downloader = new PictureDownloader();

    public static PictureDownloader getInstance() {
        return downloader;
    }

    // Create a service for downloading the picture
    private final ExecutorService service = newFixedThreadPool(PictureSignConfig.maxThreads);

    private final Hashtable<String, PictureData> cache = new Hashtable<>();

    private final Object mutex = new Object();

    // Downloads the picture, or returns the cached picture
    public PictureData getPicture(String url) {
        synchronized (mutex) {
            // Try to get the picture from cache
            PictureData data = this.cache.get(url);
            if (data == null) {
                // Download the picture if not in cache
                this.loadPicture(url);
                return null;
            }

            if (data.identifier == null) {
                return null;
            }

            return data;
        }
    }

    // Download the image and save it in cache
    private void loadPicture(String url) {
        if (url.startsWith("file:")) loadLocalPicture(url);
        else if (url.startsWith("rp:")) loadResourcePackTexture(url);
        else downloadPicture(url);
    }
    private void downloadPicture(String url) {
        if (PictureSignConfig.debug) PictureSignClient.LOGGER.info("Started downloading picture: " + url);
        this.cache.put(url, new PictureData(url));

        service.submit(() -> {
            try {
                BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
                File file = File.createTempFile("."+MOD_ID, "temp");
                file.deleteOnExit();

                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));

                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    out.write(dataBuffer, 0, bytesRead);
                }

                out.close();

                Identifier texture = convert2png(file);

                // Cache the downloaded picture
                synchronized (mutex) {
                    PictureData data = this.cache.get(url);
                    data.identifier = texture;
                }

                if (PictureSignConfig.debug) PictureSignClient.LOGGER.info("Finished downloading picture: " + url);

            } catch (IOException error) {
                PictureSignClient.LOGGER.error("Error downloading picture: " + error);
            }
        });
    }
    private void loadLocalPicture(String url) {
        String realPath = url.replace("file:", "");
        if (PictureSignConfig.debug) PictureSignClient.LOGGER.info("Started loading local picture: " + url);

        this.cache.put(url, new PictureData(url));
        service.submit(() -> {
            try {
                File file = new File(realPath);
                Identifier texture =  convert2png(file);
                synchronized (mutex) {
                    PictureData data = this.cache.get(url);
                    data.identifier = texture;
                }

            } catch (IOException error) {
                PictureSignClient.LOGGER.error("Error loading local picture: " + error);
            }
        });

        if (PictureSignConfig.debug) PictureSignClient.LOGGER.info("Finished loading local picture: " + url);
    }
    private void loadResourcePackTexture(String url) {
        String realIdentifierPath = url.replace("rp:", "");
        if (!realIdentifierPath.endsWith(".png")) realIdentifierPath += ".png";

        if (PictureSignConfig.debug) PictureSignClient.LOGGER.info("Started loading resource pack picture: " + url);
        Identifier id = Identifier.tryParse(realIdentifierPath);
        if (id == null) {
            PictureSignClient.LOGGER.error("Unable to locate resource texture: " + url);
            return;
        }

        this.cache.put(url, new PictureData(url));
        service.submit(() -> {
            synchronized (mutex) {
                PictureData data = this.cache.get(url);
                data.identifier = id;
            }
        });

        if (PictureSignConfig.debug) PictureSignClient.LOGGER.info("Finished loading resource pack picture: " + url);
    }

    private static Identifier convert2png(File file) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(file);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);

        InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

        NativeImage nativeImage = NativeImage.read(inputStream);
        NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(nativeImage);

        return MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("picturesign/image",
                nativeImageBackedTexture);
    }
}

