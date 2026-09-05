package com.codigohasta.addon.utils.leaveshack;

import com.codigohasta.addon.utils.leaveshack.events.KeyboardInputEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.util.math.MathHelper;

public class MoveFixUtil {
   public static void fixMovement(KeyboardInputEvent event, float targetYaw) {
      float forward = event.getForward();
      float strafe = event.getStrafe();
      if (forward != 0.0F || strafe != 0.0F) {
         double movementDir = getDirection(forward, strafe);
         double angleDiff = MathHelper.wrapDegrees(movementDir - targetYaw);
         double angleDist = Math.abs(angleDiff);
         float directionFactor = Math.max(Math.abs(forward), Math.abs(strafe));
         forward = 0.0F;
         strafe = 0.0F;
         if (angleDist <= 67.5) {
            forward = 1.0F;
         } else if (angleDist >= 112.5) {
            forward = -1.0F;
         }

         if (angleDiff >= 22.5 && angleDiff <= 157.5) {
            strafe = -1.0F;
         } else if (angleDiff <= -22.5 && angleDiff >= -157.5) {
            strafe = 1.0F;
         }

         forward *= directionFactor;
         strafe *= directionFactor;
         event.setForward(forward);
         event.setStrafe(strafe);
      }
   }

   private static double getDirection(float forward, float strafe) {
      float yaw = MeteorClient.mc.player.getYaw();
      if (forward == 0.0F && strafe == 0.0F) {
         return yaw;
      } else {
         boolean forward_p = forward > 0.0F;
         boolean backward_p = forward < 0.0F;
         boolean right_p = strafe > 0.0F;
         boolean left_p = strafe < 0.0F;
         if (forward_p && left_p) {
            return yaw + 45.0;
         } else if (forward_p && right_p) {
            return yaw - 45.0;
         } else if (forward_p) {
            return yaw;
         } else if (left_p) {
            return yaw + 90.0;
         } else if (right_p) {
            return yaw - 90.0;
         } else if (backward_p && left_p) {
            return yaw + 135.0;
         } else {
            return backward_p && right_p ? yaw - 135.0 : yaw + 180.0;
         }
      }
   }
}
