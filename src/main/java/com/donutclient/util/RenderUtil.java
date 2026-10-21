package com.donutclient.util;

import com.donutclient.DonutClient;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

public final class RenderUtil {
    /** Boîtes translucides visibles à travers les blocs (sans test de profondeur). */
    private static final RenderPipeline ESP_PIPELINE = RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of(DonutClient.MOD_ID, "pipeline/esp"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build();

    private static final RenderLayer ESP_LAYER = RenderLayer.of(DonutClient.MOD_ID + "_esp",
            RenderSetup.builder(ESP_PIPELINE).translucent().build());

    private RenderUtil() {
    }

    /** Couche de rendu pour des boîtes, à travers les murs ou non. */
    public static RenderLayer boxLayer(boolean throughWalls) {
        return throughWalls ? ESP_LAYER : RenderLayers.debugFilledBox();
    }

    public static void drawBox(VertexConsumer b, Matrix4f m, Box box, int color) {
        drawBox(b, m, (float) box.minX, (float) box.minY, (float) box.minZ,
                (float) box.maxX, (float) box.maxY, (float) box.maxZ, color);
    }

    public static void drawBox(VertexConsumer b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int c) {
        // Nord
        b.vertex(m, x1, y1, z1).color(c); b.vertex(m, x2, y1, z1).color(c);
        b.vertex(m, x2, y2, z1).color(c); b.vertex(m, x1, y2, z1).color(c);
        // Sud
        b.vertex(m, x2, y1, z2).color(c); b.vertex(m, x1, y1, z2).color(c);
        b.vertex(m, x1, y2, z2).color(c); b.vertex(m, x2, y2, z2).color(c);
        // Ouest
        b.vertex(m, x1, y1, z2).color(c); b.vertex(m, x1, y1, z1).color(c);
        b.vertex(m, x1, y2, z1).color(c); b.vertex(m, x1, y2, z2).color(c);
        // Est
        b.vertex(m, x2, y1, z1).color(c); b.vertex(m, x2, y1, z2).color(c);
        b.vertex(m, x2, y2, z2).color(c); b.vertex(m, x2, y2, z1).color(c);
        // Haut
        b.vertex(m, x1, y2, z1).color(c); b.vertex(m, x2, y2, z1).color(c);
        b.vertex(m, x2, y2, z2).color(c); b.vertex(m, x1, y2, z2).color(c);
        // Bas
        b.vertex(m, x1, y1, z2).color(c); b.vertex(m, x2, y1, z2).color(c);
        b.vertex(m, x2, y1, z1).color(c); b.vertex(m, x1, y1, z1).color(c);
    }
}
