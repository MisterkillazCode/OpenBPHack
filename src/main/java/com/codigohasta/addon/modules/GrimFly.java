package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.HashSet;
import java.util.Set;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent.Pre;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.util.math.Vec3d;

public class GrimFly extends Module {
   private final SettingGroup sgGeneral = this.settings.createGroup("General");
   private final SettingGroup sgBypass = this.settings.createGroup("Bypass");
   private final Setting<Integer> packets = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("packets"))
                  .description(
                     "Position packets sent per tick. The more packets, the higher the bps (speed scales linearly with packet count). Too many may trip Grim's packet-spam checks."
                  ))
               .defaultValue(8))
            .min(1)
            .max(100)
            .sliderMin(1)
            .sliderMax(100)
            .build()
      );
   private final Setting<Double> horizontalSpeed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("horizontal-speed"))
               .description("Horizontal speed per packet, in blocks per second. Effective horizontal bps = packets × this value."))
            .defaultValue(5.0)
            .min(0.1)
            .max(30.0)
            .sliderMin(0.1)
            .sliderMax(30.0)
            .build()
      );
   private final Setting<Double> verticalSpeed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("vertical-speed"))
               .description("Vertical speed per packet, in blocks per second. Effective vertical bps = packets × this value."))
            .defaultValue(1.2)
            .min(0.1)
            .max(20.0)
            .sliderMin(0.1)
            .sliderMax(20.0)
            .build()
      );
   private final Setting<Boolean> setPosition = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("set-position"))
                  .description("Moves the client player with the packets so the client matches what the server sees. Turn off for a pure packet desync fly."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> antiKick = this.sgBypass
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("anti-kick"))
                  .description("Moves down slightly every few ticks to avoid the server's flying kick."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> antiKickDelay = this.sgBypass
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("anti-kick-delay")).description("How many ticks between anti-kick dips.")).defaultValue(20))
            .min(2)
            .max(200)
            .sliderMin(2)
            .sliderMax(200)
            .build()
      );
   private final Setting<Boolean> invalidPacket = this.sgBypass
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("invalid-packet"))
                  .description("Also sends an out-of-bounds packet (y+1500) to desync the collision box / phase through blocks. Aggressive, may flag."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> cancelVanilla = this.sgBypass
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("cancel-vanilla-move"))
                  .description("Cancels the vanilla movement packet so only the spam reaches the server."))
               .defaultValue(true))
            .build()
      );
   private final Set<PlayerMoveC2SPacket> sentPackets = new HashSet<>();
   private int tickCount = 0;

   public GrimFly() {
      super(
         AddonTemplate.CATEGORY,
         "GrimFly",
         "High-velocity packet fly: spams N position packets per tick, each advancing by the velocity — bps scales with how many packets you send (highverpacket)."
      );
   }

   public void onActivate() {
      this.tickCount = 0;
      this.sentPackets.clear();
      super.onActivate();
   }

   public void onDeactivate() {
      this.sentPackets.clear();
      super.onDeactivate();
   }

   @EventHandler
   private void onGameJoin(GameJoinedEvent event) {
      this.toggle();
      this.tickCount = 0;
      this.sentPackets.clear();
   }

   @EventHandler
   private void onSendMovementPackets(Pre event) {
      if (this.mc.player != null && this.mc.getNetworkHandler() != null) {
         this.mc.player.setVelocity(0.0, 0.0, 0.0);
         boolean jump = this.mc.player.input.playerInput.jump();
         boolean sneak = this.mc.player.input.playerInput.sneak();
         double vy;
         if (jump) {
            vy = (Double)this.verticalSpeed.get() / 20.0;
         } else if (sneak) {
            vy = -(Double)this.verticalSpeed.get() / 20.0;
         } else {
            vy = 0.0;
         }

         if ((Boolean)this.antiKick.get() && !jump && ++this.tickCount % (Integer)this.antiKickDelay.get() == 0) {
            vy = -0.04;
         }

         Vec3d horizontal = PlayerUtils.getHorizontalVelocity((Double)this.horizontalSpeed.get());
         Vec3d vel = new Vec3d(horizontal.x, vy, horizontal.z);
         boolean onGround = this.mc.player.isOnGround();
         boolean horizontalCollision = this.mc.player.horizontalCollision;
         float yaw = this.mc.player.getYaw();
         float pitch = this.mc.player.getPitch();
         Vec3d pos = this.mc.player.getSyncedPos();

         for (int i = 0; i < this.packets.get(); i++) {
            pos = pos.add(vel);
            this.sendPacket(pos, yaw, pitch, onGround, horizontalCollision);
            if ((Boolean)this.invalidPacket.get()) {
               this.sendPacket(pos.add(0.0, 1500.0, 0.0), yaw, pitch, onGround, horizontalCollision);
            }
         }

         if ((Boolean)this.setPosition.get()) {
            this.mc.player.setPosition(pos.x, pos.y, pos.z);
         }
      }
   }

   @EventHandler
   private void onPacketSend(Send event) {
      if ((Boolean)this.cancelVanilla.get()) {
         if (event.packet instanceof PlayerMoveC2SPacket p && !this.sentPackets.remove(p)) {
            event.cancel();
         }
      }
   }

   private void sendPacket(Vec3d pos, float yaw, float pitch, boolean onGround, boolean horizontalCollision) {
      Full packet = new Full(pos, yaw, pitch, onGround, horizontalCollision);
      this.sentPackets.add(packet);
      this.mc.getNetworkHandler().sendPacket(packet);
   }
}
