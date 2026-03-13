package eu.midnightdust.picturesign.util;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class PictureTexture extends AbstractTexture {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private NativeImage pixels;

    public PictureTexture(Supplier<String> supplier, @NotNull NativeImage nativeImage) {
        this.pixels = nativeImage;
        //System.out.println(RenderSystem.getDevice());
        //GlDevice.
        //this.texture = RenderSystem.getDevice().createTexture(supplier, TextureFormat.RGBA8, this.pixels.getWidth(), this.pixels.getHeight(), 1);
        this.texture = new GlTexture(supplier.get(), TextureFormat.RGBA8, this.pixels.getWidth(), this.pixels.getHeight(), 1, 1);
        //this.upload();
    }
}
