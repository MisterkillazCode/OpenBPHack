package com.codigohasta.addon.utils.bmw;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class BMWPlayerUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static boolean isMoving() {
      return BMWDirectionalInput.fromPlayer().isMoving();
   }

   public static float getMovementDirectionOfInput(float facingYaw, BMWDirectionalInput input) {
      boolean forwards = input.forwards && !input.backwards;
      boolean backwards = input.backwards && !input.forwards;
      boolean left = input.left && !input.right;
      boolean right = input.right && !input.left;
      float actualYaw = facingYaw;
      float fwd = 1.0F;
      if (backwards) {
         actualYaw = facingYaw + 180.0F;
         fwd = -0.5F;
      } else if (forwards) {
         fwd = 0.5F;
      }

      if (left) {
         actualYaw -= 90.0F * fwd;
      }

      if (right) {
         actualYaw += 90.0F * fwd;
      }

      return MathHelper.wrapDegrees(actualYaw);
   }

   public static float getMovementDirectionOfInput(float facingYaw) {
      return getMovementDirectionOfInput(facingYaw, BMWDirectionalInput.fromPlayer());
   }
}
