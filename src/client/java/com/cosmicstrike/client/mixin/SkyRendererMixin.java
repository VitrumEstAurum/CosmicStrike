package com.cosmicstrike.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SkyRenderer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    private static final int POINT_COUNT = 8;
    private static final float RADIUS = 18.0f;
    private static final float HALF_SIZE = 0.8f;

    private static GpuBuffer cosmicStrike$vertices;

    @Inject(method = "renderStars", at = @At("TAIL"))
    private void cosmicStrike$renderPoints(
        RenderPass pass,
        float brightness,
        PoseStack poseStack,
        CallbackInfo ci
    ) {
        if (cosmicStrike$vertices == null) {
            try (ByteBufferBuilder builder = new ByteBufferBuilder(1024)) {
                BufferBuilder meshBuilder = new BufferBuilder(
                    builder,
                    PrimitiveTopology.QUADS,
                    DefaultVertexFormat.POSITION_COLOR
                );

                for (int i = 0; i < POINT_COUNT; i++) {
                    double angle = 2.0 * Math.PI * i / POINT_COUNT;
                    float x = (float) (RADIUS * Math.cos(angle));
                    float y = (float) (RADIUS * Math.sin(angle));
                    float z = -100.0f;

                    meshBuilder.addVertex(x - HALF_SIZE, y - HALF_SIZE, z)
                        .setColor(255, 8, 16, 255);
                    meshBuilder.addVertex(x + HALF_SIZE, y - HALF_SIZE, z)
                        .setColor(255, 8, 16, 255);
                    meshBuilder.addVertex(x + HALF_SIZE, y + HALF_SIZE, z)
                        .setColor(255, 8, 16, 255);
                    meshBuilder.addVertex(x - HALF_SIZE, y + HALF_SIZE, z)
                        .setColor(255, 8, 16, 255);
                }

                try (MeshData mesh = meshBuilder.buildOrThrow()) {
                    cosmicStrike$vertices = RenderSystem.getDevice().createBuffer(
                        () -> "Cosmic Strike sky points",
                        GpuBuffer.USAGE_VERTEX,
                        mesh.vertexBuffer()
                    );
                }
            }
        }

        Matrix4f transform = new Matrix4f(
            RenderSystem.getModelViewStack()
        ).mul(poseStack.last().pose());

        var uniforms = RenderSystem.getDynamicUniforms().writeTransform(
            transform,
            new Vector4f(1, 1, 1, 1)
        );

        pass.setPipeline(
            RenderSystem.getCompiledPipeline(RenderPipelines.DEBUG_QUADS)
        );
        RenderSystem.bindDefaultUniforms(pass);
        pass.setUniform("DynamicTransforms", uniforms);
        pass.setVertexBuffer(0, cosmicStrike$vertices.slice());
        pass.draw(0, POINT_COUNT * 4, 1, 0);
    }
}

