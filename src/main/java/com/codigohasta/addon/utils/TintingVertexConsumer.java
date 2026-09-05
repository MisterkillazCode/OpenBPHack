package com.codigohasta.addon.utils;

import net.minecraft.client.render.VertexConsumer;

public class TintingVertexConsumer implements VertexConsumer {
   private final VertexConsumer delegate;
   private final float tintR;
   private final float tintG;
   private final float tintB;
   private final float tintA;

   public TintingVertexConsumer(VertexConsumer delegate, float r, float g, float b, float a) {
      this.delegate = delegate;
      this.tintR = r;
      this.tintG = g;
      this.tintB = b;
      this.tintA = a;
   }

   public VertexConsumer vertex(float x, float y, float z) {
      return this.delegate.vertex(x, y, z);
   }

   public VertexConsumer color(int red, int green, int blue, int alpha) {
      return this.delegate
         .color(
            Math.min(255, (int)(red * this.tintR)),
            Math.min(255, (int)(green * this.tintG)),
            Math.min(255, (int)(blue * this.tintB)),
            Math.min(255, (int)(alpha * this.tintA))
         );
   }

   public VertexConsumer color(int argb) {
      int a = argb >> 24 & 0xFF;
      int r = argb >> 16 & 0xFF;
      int g = argb >> 8 & 0xFF;
      int b = argb & 0xFF;
      int newR = Math.min(255, (int)(r * this.tintR));
      int newG = Math.min(255, (int)(g * this.tintG));
      int newB = Math.min(255, (int)(b * this.tintB));
      int newA = Math.min(255, (int)(a * this.tintA));
      return this.delegate.color(newA << 24 | newR << 16 | newG << 8 | newB);
   }

   public VertexConsumer texture(float u, float v) {
      this.delegate.texture(u, v);
      return this;
   }

   public VertexConsumer overlay(int u, int v) {
      this.delegate.overlay(u, v);
      return this;
   }

   public VertexConsumer light(int u, int v) {
      this.delegate.light(u, v);
      return this;
   }

   public VertexConsumer normal(float x, float y, float z) {
      this.delegate.normal(x, y, z);
      return this;
   }

   public VertexConsumer lineWidth(float width) {
      this.delegate.lineWidth(width);
      return this;
   }
}
