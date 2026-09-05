package com.codigohasta.addon.utils.leaveshack.events;

import net.minecraft.client.util.math.MatrixStack;

public class RenderLeaves3DEvent {
   private static final RenderLeaves3DEvent INSTANCE = new RenderLeaves3DEvent();
   public MatrixStack matrixStack;
   public float tickDelta;

   public static RenderLeaves3DEvent get(MatrixStack matrixStack, float tickDelta) {
      INSTANCE.matrixStack = matrixStack;
      INSTANCE.tickDelta = tickDelta;
      return INSTANCE;
   }
}
