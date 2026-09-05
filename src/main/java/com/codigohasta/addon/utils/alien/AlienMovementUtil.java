package com.codigohasta.addon.utils.alien;

import net.minecraft.client.MinecraftClient;

public class AlienMovementUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static boolean isMoving() {
      return mc.player == null
         ? false
         : mc.player.input.playerInput.forward()
            || mc.player.input.playerInput.backward()
            || mc.player.input.playerInput.left()
            || mc.player.input.playerInput.right();
   }

   public static double getMotionX() {
      return mc.player.getVelocity().x;
   }

   public static void setMotionX(double x) {
      mc.player.setVelocity(x, mc.player.getVelocity().y, mc.player.getVelocity().z);
   }

   public static double getMotionY() {
      return mc.player.getVelocity().y;
   }

   public static void setMotionY(double y) {
      mc.player.setVelocity(mc.player.getVelocity().x, y, mc.player.getVelocity().z);
   }

   public static double getMotionZ() {
      return mc.player.getVelocity().z;
   }

   public static void setMotionZ(double z) {
      mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y, z);
   }

   public static double[] directionSpeed(double speed) {
      if (mc.player == null) {
         return new double[]{0.0, 0.0};
      } else {
         float forward = mc.player.input.playerInput.forward() ? 1.0F : (mc.player.input.playerInput.backward() ? -1.0F : 0.0F);
         float side = mc.player.input.playerInput.left() ? 1.0F : (mc.player.input.playerInput.right() ? -1.0F : 0.0F);
         float yaw = mc.player.getYaw();
         if (forward == 0.0F && side == 0.0F) {
            return new double[]{0.0, 0.0};
         } else {
            if (forward != 0.0F) {
               if (side > 0.0F) {
                  yaw += forward > 0.0F ? -45 : 45;
               } else if (side < 0.0F) {
                  yaw += forward > 0.0F ? 45 : -45;
               }

               side = 0.0F;
               if (forward > 0.0F) {
                  forward = 1.0F;
               } else if (forward < 0.0F) {
                  forward = -1.0F;
               }
            }

            double sin = Math.sin(Math.toRadians(yaw + 90.0F));
            double cos = Math.cos(Math.toRadians(yaw + 90.0F));
            double posX = forward * speed * cos + side * speed * sin;
            double posZ = forward * speed * sin - side * speed * cos;
            return new double[]{posX, posZ};
         }
      }
   }
}
