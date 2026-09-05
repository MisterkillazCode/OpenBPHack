package com.codigohasta.addon.utils.bmw;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class BMWRotationManager {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public static BMWRotation targetRotation = null;
   public static boolean changeLook = false;

   public static void setRotationTarget(BMWRotation rotation, boolean changeLookMode) {
      targetRotation = rotation;
      changeLook = changeLookMode;
   }

   public static void clearTarget() {
      targetRotation = null;
      changeLook = false;
   }

   public static void update() {
      if (targetRotation != null) {
         if (mc.player != null) {
            if (changeLook) {
               setPlayerRotation(mc.player, targetRotation);
            }
         }
      }
   }

   public static void setPlayerRotation(ClientPlayerEntity player, BMWRotation rotation) {
      BMWRotation normalized = rotation.normalize();
      player.setYaw(normalized.yaw);
      player.setPitch(normalized.pitch);
   }
}
