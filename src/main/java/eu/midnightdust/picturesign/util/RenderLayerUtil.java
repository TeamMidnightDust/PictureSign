package eu.midnightdust.picturesign.util;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.mixin.TextureManagerAccessor;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

import java.util.function.BiFunction;
import java.util.function.Function;

public class RenderLayerUtil {
    public static final Function<ResourceLocation, RenderType> TEXTURED_RENDER_LAYER = Util.memoize(RenderLayerUtil::createTexturedRenderLayer);
    public static final BiFunction<ResourceLocation, Integer, RenderType> GL_RENDER_LAYER = Util.memoize(RenderLayerUtil::createGlRenderLayer);

    private static RenderType createGlRenderLayer(ResourceLocation videoId, int glId) {
        if (!((TextureManagerAccessor) Minecraft.getInstance().getTextureManager()).getTextures().containsKey(videoId))
            Minecraft.getInstance().getTextureManager().register(videoId, new PictureSignTexture(videoId, glId));
        return createTexturedRenderLayer(videoId);
    }

    private static RenderType createTexturedRenderLayer(ResourceLocation texture) {
        boolean blendEnabled = PictureSignConfig.translucency;

        return RenderType.create("picturesign_%s_%s".formatted(texture.getPath(), blendEnabled), 1536, blendEnabled ? RenderPipelines.TRANSLUCENT : RenderPipelines.CUTOUT,//PIPELINE_BUILDER.build(),
                RenderType.CompositeState.builder()
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, TriState.FALSE, false))
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        //.setOutputState(RenderStateShard.TRANSLUCENT_TARGET)
                        .createCompositeState(false));
    }

    private static class PictureSignTexture extends AbstractTexture {
        private PictureSignTexture(ResourceLocation videoId, int glId) {
            this.texture = new GlTexture(videoId.toString(), TextureFormat.RGBA8, 16, 16, 1, glId);
            //this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
        }
    }
}
