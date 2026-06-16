package me.grish.veinforge.util.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Small set of render pipelines used for world-space debug rendering.
 * <p>
 * These are intended for drawing simple colored primitives (quads/lines) with
 * predictable behavior (e.g., no depth testing for tracers).
 */
public final class VFRenderPipelines {

   public static final RenderPipeline QUADS_DEPTH = RenderPipelines.register(
           RenderPipeline.builder()
                   .withVertexShader("core/position_color")
                   .withFragmentShader("core/position_color")
                   .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                   .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                   .withPrimitiveTopology(PrimitiveTopology.QUADS)
                   .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                   .withCull(false)
                   .withLocation(Identifier.fromNamespaceAndPath("veinforge", "pipeline/quads_depth"))
                   .build()
   );

   public static final RenderPipeline QUADS_NO_DEPTH = RenderPipelines.register(
           RenderPipeline.builder()
                   .withVertexShader("core/position_color")
                   .withFragmentShader("core/position_color")
                   .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                   .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                   .withPrimitiveTopology(PrimitiveTopology.QUADS)
                   .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                   .withCull(false)
                   .withLocation(Identifier.fromNamespaceAndPath("veinforge", "pipeline/quads_no_depth"))
                   .build()
   );

   public static final RenderPipeline LINES_NO_DEPTH = RenderPipelines.register(
           RenderPipeline.builder()
                   .withVertexShader("core/position_color")
                   .withFragmentShader("core/position_color")
                   .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                   .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                   .withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES)
                   .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                   .withCull(false)
                   .withLocation(Identifier.fromNamespaceAndPath("veinforge", "pipeline/lines_no_depth"))
                   .build()
   );

   private VFRenderPipelines() {
   }
}
