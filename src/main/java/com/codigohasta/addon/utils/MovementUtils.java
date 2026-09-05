package com.codigohasta.addon.utils;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.Vec3d;

public class MovementUtils {
   private static final double MAX_STEP = 6.0;

   public static void tpMove(ClientPlayerEntity player, Vec3d start, Vec3d end, boolean onGround) {
      if (player != null) {
         Vec3d diff = end.subtract(start);
         double distance = diff.length();
         int steps = (int)Math.ceil(distance / 6.0);
         if (steps < 1) {
            steps = 1;
         }

         Vec3d stepVector = diff.multiply(1.0 / steps);
         Vec3d currentPos = start;

         for (int i = 0; i < steps; i++) {
            Vec3d nextPos = currentPos.add(stepVector);
            player.networkHandler.sendPacket(new PositionAndOnGround(nextPos.getX(), nextPos.getY(), nextPos.getZ(), onGround, true));
            player.networkHandler.sendPacket(new PositionAndOnGround(nextPos.getX(), nextPos.getY(), nextPos.getZ(), onGround, true));
            currentPos = nextPos;
         }

         player.setPosition(end.x, end.y, end.z);
      }
   }
}
