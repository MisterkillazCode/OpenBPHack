package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent.Post;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

public class Grim2Speed extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> highPingMode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("high-ping-mode"))
                  .description("Alternative double-packet pattern. May be slower, but works more consistently on high ping."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Double> speed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("speed"))
               .description("Strafe acceleration multiplier (0-1)."))
            .defaultValue(1.0)
            .min(0.0)
            .max(1.0)
            .sliderMin(0.0)
            .sliderMax(1.0)
            .build()
      );
   private boolean shouldJump;
   private int strafeTicks;

   public Grim2Speed() {
      super(
         AddonTemplate.CATEGORY,
         "Grim2Speed",
         "Grim2-style speed: sends a double onGround packet every other tick and jumps on knockback to keep momentum. Ping below 150ms recommended. For self-hosted / LAN PvP only."
      );
   }

   public void onActivate() {
      if (this.mc.player != null && this.mc.getNetworkHandler() != null) {
         this.strafeTicks = 0;
         this.shouldJump = false;
         this.sendDoublePacket();
      } else {
         this.toggle();
      }
   }

   private void sendDoublePacket() {
      if (this.mc.getNetworkHandler() != null) {
         boolean hc = this.mc.player != null && this.mc.player.horizontalCollision;
         if (!(Boolean)this.highPingMode.get()) {
            this.mc.getNetworkHandler().sendPacket(new OnGroundOnly(true, hc));
            this.mc.getNetworkHandler().sendPacket(new OnGroundOnly(false, hc));
         } else {
            this.mc.getNetworkHandler().sendPacket(new OnGroundOnly(false, hc));
            this.mc.getNetworkHandler().sendPacket(new OnGroundOnly(false, hc));
         }
      }
   }

   @EventHandler
   private void onPreTick(Pre event) {
      if (this.mc.player != null) {
         this.shouldJump = false;
         if (this.strafeTicks > -1) {
            double accel = 0.03;
            if (this.strafeTicks % 2 == 0) {
               accel = this.mc.player.isOnGround() ? 0.085 : 0.03;
            }

            this.strafe(accel * (Double)this.speed.get());
         }

         this.strafeTicks++;
      }
   }

   @EventHandler
   private void onSendMovementPacketsPost(Post event) {
      if (this.strafeTicks % 2 == 0) {
         this.sendDoublePacket();
      }
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      if (this.mc.player != null) {
         if (event.packet instanceof PlayerPositionLookS2CPacket) {
            if (this.strafeTicks % 2 == 1) {
               this.strafeTicks++;
            }
         } else if (event.packet instanceof EntityVelocityUpdateS2CPacket packet && packet.getEntityId() == this.mc.player.getId()) {
            this.shouldJump = true;
         }
      }
   }

   @EventHandler
   private void onPostTick(meteordevelopment.meteorclient.events.world.TickEvent.Post event) {
      if (this.shouldJump && this.mc.player != null && this.mc.player.isOnGround()) {
         this.mc.player.jump();
      }
   }

   private void strafe(double accel) {
      if (PlayerUtils.isMoving()) {
         Vec3d v = this.mc.player.getVelocity();
         double bps = Math.hypot(v.x, v.z) * 20.0;
         Vec3d target = PlayerUtils.getHorizontalVelocity(bps + accel * 20.0);
         this.mc.player.setVelocity(target.x, v.y, target.z);
      }
   }
}
