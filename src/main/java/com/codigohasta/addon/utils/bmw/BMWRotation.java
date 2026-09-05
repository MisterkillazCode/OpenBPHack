package com.codigohasta.addon.utils.bmw;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class BMWRotation {
   public static final BMWRotation ZERO = new BMWRotation(0.0F, 0.0F);
   public final float yaw;
   public final float pitch;

   public BMWRotation(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public BMWRotation normalize() {
      return new BMWRotation(MathHelper.wrapDegrees(this.yaw), MathHelper.wrapDegrees(this.pitch));
   }

   public static BMWRotation ofPlayer() {
      MinecraftClient mc = MinecraftClient.getInstance();
      return mc.player == null ? ZERO : new BMWRotation(mc.player.getYaw(), mc.player.getPitch());
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof BMWRotation other) ? false : Float.compare(other.yaw, this.yaw) == 0 && Float.compare(other.pitch, this.pitch) == 0;
      }
   }

   @Override
   public int hashCode() {
      return 31 * Float.hashCode(this.yaw) + Float.hashCode(this.pitch);
   }

   @Override
   public String toString() {
      return "BMWRotation{yaw=" + this.yaw + ", pitch=" + this.pitch + "}";
   }
}
