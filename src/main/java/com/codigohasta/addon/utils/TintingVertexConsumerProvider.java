package com.codigohasta.addon.utils;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

public class TintingVertexConsumerProvider implements VertexConsumerProvider {
   private final VertexConsumerProvider delegate;
   private final float r;
   private final float g;
   private final float b;
   private final float a;

   public TintingVertexConsumerProvider(VertexConsumerProvider delegate, float r, float g, float b, float a) {
      this.delegate = delegate;
      this.r = r;
      this.g = g;
      this.b = b;
      this.a = a;
   }

   public VertexConsumer getBuffer(RenderLayer layer) {
      return new TintingVertexConsumer(this.delegate.getBuffer(layer), this.r, this.g, this.b, this.a);
   }
}
