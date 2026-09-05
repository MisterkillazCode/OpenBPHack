package com.codigohasta.addon.utils.leaveshack;

import com.codigohasta.addon.modules.GlobalSetting;
import com.codigohasta.addon.utils.leaveshack.events.KeyboardInputEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Rotation {
   public static final Rotation INSTANCE = new Rotation();
   public static float rotationYaw = 0.0F;
   public static float rotationPitch = 0.0F;
   public static boolean rotation = false;
   public static float targetYaw = 0.0F;
   public static float targetPitch = 0.0F;
   public static float lastYaw = 0.0F;
   public static float lastPitch = 0.0F;
   public static boolean lastGround;

   private Rotation() {
      MeteorClient.EVENT_BUS.subscribe(this);
   }

   public static void snapAt(float yaw, float pitch) {
      if ((Boolean)GlobalSetting.INSTANCE.moveFix.get()) {
         rotation = true;
         targetPitch = pitch;
         targetYaw = yaw;
      } else {
         rotationYaw = MeteorClient.mc.player.getYaw();
         rotationPitch = MeteorClient.mc.player.getPitch();
         if ((Boolean)GlobalSetting.INSTANCE.grimRotation.get()) {
            sendPacket(new LookAndOnGround(yaw, pitch, MeteorClient.mc.player.isOnGround(), MeteorClient.mc.player.horizontalCollision));
         } else {
            sendPacket(new LookAndOnGround(yaw, pitch, MeteorClient.mc.player.isOnGround(), MeteorClient.mc.player.horizontalCollision));
         }
      }
   }

   public static void snapBack() {
      if ((Boolean)GlobalSetting.INSTANCE.snapBack.get()) {
         if (!(Boolean)GlobalSetting.INSTANCE.moveFix.get()) {
            sendPacket(new LookAndOnGround(rotationYaw, rotationPitch, MeteorClient.mc.player.isOnGround(), MeteorClient.mc.player.horizontalCollision));
         }
      }
   }

   @EventHandler
   public void onKeyInput(KeyboardInputEvent event) {
      if (rotation) {
         MoveFixUtil.fixMovement(event, targetYaw);
      }
   }

   public static void sendPacket(Packet<?> packet) {
      MeteorClient.mc.getNetworkHandler().sendPacket(packet);
   }

   public static void snapAt(Vec3d directionVec) {
      float[] angle = getRotation(directionVec);
      snapAt(angle[0], angle[1]);
   }

   public static void snapAt(Box box) {
      snapAt(getClosestPointToEye(MeteorClient.mc.player.getEyePos(), box));
   }

   @EventHandler(
      priority = -999
   )
   public void onPacketSend(Send event) {
      if (MeteorClient.mc.player != null && !event.isCancelled()) {
         if (event.packet instanceof PlayerMoveC2SPacket packet) {
            if (packet.changesLook()) {
               lastYaw = packet.getYaw(lastYaw);
               lastPitch = packet.getPitch(lastPitch);
            }

            lastGround = packet.isOnGround();
         }
      }
   }

   @EventHandler(
      priority = 100
   )
   public void onReceivePacket(Receive event) {
      if (MeteorClient.mc.player != null) {
         if (event.packet instanceof PlayerPositionLookS2CPacket packet) {
            if (packet.relatives().contains(PositionFlag.X_ROT)) {
               lastYaw = lastYaw + packet.change().yaw();
            } else {
               lastYaw = packet.change().yaw();
            }

            if (packet.relatives().contains(PositionFlag.Y_ROT)) {
               lastPitch = lastPitch + packet.change().pitch();
            } else {
               lastPitch = packet.change().pitch();
            }
         }
      }
   }

   public static Vec3d getClosestPointToEye(Vec3d eyePos, Box box) {
      double x = eyePos.x;
      double y = eyePos.y;
      double z = eyePos.z;
      if (eyePos.x < box.minX) {
         x = box.minX;
      } else if (eyePos.x > box.maxX) {
         x = box.maxX;
      }

      if (eyePos.y < box.minY) {
         y = box.minY;
      } else if (eyePos.y > box.maxY) {
         y = box.maxY;
      }

      if (eyePos.z < box.minZ) {
         z = box.minZ;
      } else if (eyePos.z > box.maxZ) {
         z = box.maxZ;
      }

      return new Vec3d(x, y, z);
   }

   public static float[] getRotation(Vec3d eyesPos, Vec3d vec) {
      double diffX = vec.x - eyesPos.x;
      double diffY = vec.y - eyesPos.y;
      double diffZ = vec.z - eyesPos.z;
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
      return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
   }

   public static float[] getRotation(Vec3d vec) {
      Vec3d eyesPos = MeteorClient.mc.player.getEyePos();
      return getRotation(eyesPos, vec);
   }
}
