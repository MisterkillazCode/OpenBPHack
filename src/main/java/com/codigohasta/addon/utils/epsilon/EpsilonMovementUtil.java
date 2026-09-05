package com.codigohasta.addon.utils.epsilon;

import java.lang.reflect.Field;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;

public class EpsilonMovementUtil {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static Field yField;
   private static Field onGroundField;

   public static void setOnGround(PlayerMoveC2SPacket packet, boolean onGround) {
      if (onGroundField != null) {
         try {
            onGroundField.setBoolean(packet, onGround);
         } catch (Exception var3) {
         }
      }
   }

   public static double getY(PlayerMoveC2SPacket packet) {
      if (yField != null) {
         try {
            return yField.getDouble(packet);
         } catch (Exception var2) {
         }
      }

      return 0.0;
   }

   public static void setY(PlayerMoveC2SPacket packet, double y) {
      if (yField != null) {
         try {
            yField.setDouble(packet, y);
         } catch (Exception var4) {
         }
      }
   }

   public static void shiftY(PlayerMoveC2SPacket packet, double delta) {
      setY(packet, getY(packet) + delta);
   }

   public static boolean hasPosition(PlayerMoveC2SPacket packet) {
      return packet instanceof PositionAndOnGround || packet instanceof Full;
   }

   public static PlayerMoveC2SPacket createGrimPositionPacket(double delta) {
      assert mc.player != null;

      return new PositionAndOnGround(
         mc.player.getX(), mc.player.getY() + delta, mc.player.getZ(), true, mc.player.horizontalCollision
      );
   }

   public static void setJump(boolean jump) {
      if (mc.player != null) {
         mc.options.jumpKey.setPressed(jump);
      }
   }

   static {
      try {
         for (Field f : PlayerMoveC2SPacket.class.getDeclaredFields()) {
            f.setAccessible(true);
            String name = f.getName();
            if (f.getType() == double.class && name.equals("y")) {
               yField = f;
            } else if (f.getType() == boolean.class && name.equals("onGround")) {
               onGroundField = f;
            }
         }
      } catch (Exception var5) {
      }
   }
}
