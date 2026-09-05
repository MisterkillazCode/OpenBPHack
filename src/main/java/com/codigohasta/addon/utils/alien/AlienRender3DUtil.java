package com.codigohasta.addon.utils.alien;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class AlienRender3DUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final List<AlienRender3DUtil.TextRequest> textQueue = new ArrayList<>();
   private static final BufferAllocator textBuffer = new BufferAllocator(2048);
   private static final Immediate textImmediate = VertexConsumerProvider.immediate(textBuffer);
   private static final Matrix4f identityMatrix = new Matrix4f();
   private static final Vector3d projPos = new Vector3d();

   public static void drawText3D(String text, Vec3d vec3d, int color) {
      drawText3D(Text.of(text), vec3d, 0.0, 0.0, 1.0, color, 2.0, 0.5, Double.MAX_VALUE);
   }

   public static void drawText3D(String text, Vec3d vec3d, double baseScale, double distanceFactor, double maxScale, int color) {
      drawText3D(Text.of(text), vec3d, 0.0, 0.0, 1.0, color, baseScale, distanceFactor, maxScale);
   }

   public static void drawText3D(Text text, Vec3d vec3d, double offX, double offY, double scale, int color) {
      drawText3D(text, vec3d, offX, offY, scale, color, 2.0, 0.5, Double.MAX_VALUE);
   }

   public static void drawText3D(Text text, Vec3d vec3d, double offX, double offY, double scale, int color, double maxScale) {
      drawText3D(text, vec3d, offX, offY, scale, color, 2.0, 0.5, maxScale);
   }

   public static void drawText3D(
      Text text, Vec3d vec3d, double offX, double offY, double scale, int color, double baseScale, double distanceFactor, double maxScale
   ) {
      textQueue.add(new AlienRender3DUtil.TextRequest(text, vec3d.add(offX, offY, 0.0), scale, color, baseScale, distanceFactor, maxScale));
   }

   public static void renderDeferred() {
      if (!textQueue.isEmpty()) {
         for (AlienRender3DUtil.TextRequest req : textQueue) {
            projPos.set(req.position.x, req.position.y, req.position.z);
            if (NametagUtils.to2D(projPos, 2.0, false)) {
               double dist = Math.sqrt(mc.gameRenderer.getCamera().getCameraPos().squaredDistanceTo(req.position));
               double scale = Math.min(req.baseScale + dist * req.distanceFactor, req.maxScale);
               NametagUtils.scale = scale;
               NametagUtils.begin(projPos);
               int halfWidth = mc.textRenderer.getWidth(req.text) / 2;
               float scaledX = -halfWidth / (float)scale;
               mc.textRenderer.draw(req.text, scaledX, 0.0F, req.color, true, identityMatrix, textImmediate, TextLayerType.NORMAL, 0, 15728880);
               textImmediate.draw();
               NametagUtils.end();
            }
         }

         textQueue.clear();
      }
   }

   public static void drawFill(Render3DEvent event, Box bb, Color fillColor) {
      if (fillColor != null) {
         meteordevelopment.meteorclient.utils.render.color.Color meteorColor = new meteordevelopment.meteorclient.utils.render.color.Color(
            fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), fillColor.getAlpha()
         );
         event.renderer.box(bb, meteorColor, meteorColor, ShapeMode.Sides, 0);
      }
   }

   public static void drawBox(Render3DEvent event, Box bb, Color outlineColor) {
      if (outlineColor != null) {
         meteordevelopment.meteorclient.utils.render.color.Color meteorColor = new meteordevelopment.meteorclient.utils.render.color.Color(
            outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), outlineColor.getAlpha()
         );
         event.renderer.box(bb, meteorColor, meteorColor, ShapeMode.Lines, 0);
      }
   }

   public static void draw3DBox(Render3DEvent event, Box box, Color fillColor, Color outlineColor, boolean outline, boolean fill) {
      if (fill && fillColor != null) {
         drawFill(event, box, fillColor);
      }

      if (outline && outlineColor != null) {
         drawBox(event, box, outlineColor);
      }
   }

   public static MatrixStack matrixFrom(double x, double y, double z) {
      MatrixStack matrices = new MatrixStack();
      Camera camera = mc.gameRenderer.getCamera();
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      Vec3d camPos = camera.getCameraPos();
      matrices.translate(x - camPos.x, y - camPos.y, z - camPos.z);
      return matrices;
   }

   private record TextRequest(Text text, Vec3d position, double scale, int color, double baseScale, double distanceFactor, double maxScale) {
   }
}
