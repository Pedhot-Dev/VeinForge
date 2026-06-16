package me.grish.veinforge.util.render;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * RenderLayers backed by {@link VFRenderPipelines}.
 */
public final class VFRenderLayers {

   /**
    * World-space quads with blending, no cull, depth test enabled.
    */
   public static final RenderType QUADS_DEPTH = RenderType.create(
           "veinforge:quads_depth",
            RenderSetup.builder(VFRenderPipelines.QUADS_DEPTH)
                    .sortOnUpload()
                    .createRenderSetup()
   );

   /**
    * World-space quads with blending, no cull, no depth test.
    * Useful for tracer ribbons that should be visible through blocks.
    */
   public static final RenderType QUADS_NO_DEPTH = RenderType.create(
           "veinforge:quads_no_depth",
            RenderSetup.builder(VFRenderPipelines.QUADS_NO_DEPTH)
                    .sortOnUpload()
                    .createRenderSetup()
   );

   /**
    * World-space debug lines with no depth test.
    * Note: line width support is driver-dependent; prefer QUADS_NO_DEPTH for consistent thickness.
    */
   public static final RenderType LINES_NO_DEPTH = RenderType.create(
           "veinforge:lines_no_depth",
            RenderSetup.builder(VFRenderPipelines.LINES_NO_DEPTH)
                    .sortOnUpload()
                    .createRenderSetup()
   );

   private VFRenderLayers() {
   }
}
