package eu.midnightdust.picturesign.util;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.TextureFormat;
import eu.midnightdust.picturesign.mixin.RenderPipelinesAccessor;
import eu.midnightdust.picturesign.mixin.TextureManagerAccessor;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;

import static eu.midnightdust.picturesign.PictureSignClient.client;

public class RenderLayerUtil {
//    public static RenderLayer PICTURESIGN_RENDER_LAYER;
//    public static RenderLayer PICTURESIGN_RENDER_LAYER_TRANSLUCENT;
//
//    public static RenderLayer getCustomLogoRenderLayer(boolean blendEnabled) {
//        return blendEnabled ? PICTURESIGN_RENDER_LAYER_TRANSLUCENT : PICTURESIGN_RENDER_LAYER;
//    }
//
//    public static void buildRenderLayers() {
//        PICTURESIGN_RENDER_LAYER = buildRenderLayer(false);
//        PICTURESIGN_RENDER_LAYER_TRANSLUCENT = buildRenderLayer(true);
//    }
    public static RenderLayer buildRenderLayer(Identifier videoId, int glId, boolean blendEnabled) {
        if (!((TextureManagerAccessor) client.getTextureManager()).getTextures().containsKey(videoId))
            client.getTextureManager().registerTexture(videoId, new PictureSignTexture(glId));
        return buildRenderLayer(videoId, blendEnabled);
    }

    public static RenderLayer buildRenderLayer(Identifier texture, boolean blendEnabled) {
        var PIPELINE_BUILDER = RenderPipeline.builder(RenderPipelinesAccessor.getPOSITION_TEX_COLOR_SNIPPET())
                .withLocation("pipeline/picturesign_%s_%s".formatted(texture.getPath(), blendEnabled))
                .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
                .withDepthWrite(true);

        PIPELINE_BUILDER = blendEnabled ? PIPELINE_BUILDER.withBlend(BlendFunction.TRANSLUCENT) : PIPELINE_BUILDER.withoutBlend();

        return RenderLayer.of("picturesign_%s_%s".formatted(texture.getPath(), blendEnabled), 786432, PIPELINE_BUILDER.build(),
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(new RenderPhase.Texture(texture, TriState.DEFAULT, false))
                        .lightmap(new RenderPhase.Lightmap(true))
                        .overlay(new RenderPhase.Overlay(true))
                        .build(false));
    }
    public static class PictureSignTexture extends AbstractTexture {
        public PictureSignTexture(int glId) {
            this.glTexture = new GlTexture("abc", TextureFormat.RGBA8, 16, 16, 1, glId);
        }
    }
}
