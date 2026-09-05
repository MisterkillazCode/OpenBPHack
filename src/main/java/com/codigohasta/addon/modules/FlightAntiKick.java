package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.Random;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.Vec3d;

public class FlightAntiKick extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRandom = this.settings.createGroup("Random (more)");
   private final Setting<FlightAntiKick.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("AntiMode"))
                  .description("ThingLogicdown: DirectSpeed; DataPackdown: OnlygiveServerSenddownPack; VerticalHeavyPower: Holdweakdown."))
               .defaultValue(FlightAntiKick.Mode.PhysicalSink))
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("BasicRate"))
                  .description("TriggerAntiMoveDo'sBetween. Suggest25-35."))
               .defaultValue(25))
            .min(5)
            .build()
      );
   private final Setting<Double> dipDistance = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("downDegree"))
               .description("downBitMove'sDistance. Suggest 0.04(thisarepastCheckTest'sYellowGoldNumber)."))
            .defaultValue(0.04)
            .min(0.01)
            .sliderMax(0.1)
            .build()
      );
   private final Setting<Boolean> onlyIfActive = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("MoveFlyModule"))
                  .description("at Meteor Flight EnableTimeSpawnEffect."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> useRandom = this.sgRandom
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("RandomDelay"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> randomRange = this.sgRandom
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("RandomRange"))
                  .defaultValue(5))
               .min(1)
               .visible(this.useRandom::get))
            .build()
      );
   private final Random random = new Random();
   private int timer = 0;
   private int currentMaxDelay;

   public FlightAntiKick() {
      super(AddonTemplate.CATEGORY, "FlightAntiKick", "Anti-kick for creative flight; keeps you from being kicked.");
   }

   public void onActivate() {
      this.timer = 0;
      this.resetDelay();
   }

   private void resetDelay() {
      this.currentMaxDelay = this.useRandom.get()
         ? (Integer)this.delay.get() + (this.random.nextInt((Integer)this.randomRange.get() * 2) - (Integer)this.randomRange.get())
         : (Integer)this.delay.get();
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null) {
         if ((Boolean)this.onlyIfActive.get()) {
            Module flight = Modules.get().get("flight");
            if (flight == null || !flight.isActive()) {
               return;
            }
         }

         if (!this.mc.player.isOnGround()) {
            if (this.mode.get() == FlightAntiKick.Mode.VerticalGravity) {
               Vec3d v = this.mc.player.getVelocity();
               if (v.y >= 0.0) {
                  this.mc.player.setVelocity(v.x, -0.005, v.z);
               }
            } else {
               this.timer++;
               if (this.timer >= this.currentMaxDelay) {
                  this.executeKickAction();
                  this.timer = 0;
                  this.resetDelay();
               }
            }
         }
      }
   }

   private void executeKickAction() {
      double dip = (Double)this.dipDistance.get();
      if (this.mode.get() == FlightAntiKick.Mode.PhysicalSink) {
         Vec3d v = this.mc.player.getVelocity();
         this.mc.player.setVelocity(v.x, -dip, v.z);
      } else if (this.mode.get() == FlightAntiKick.Mode.PacketDive) {
         double x = this.mc.player.getX();
         double y = this.mc.player.getY();
         double z = this.mc.player.getZ();
         this.mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y - dip, z, false, this.mc.player.horizontalCollision));
      }
   }

   public String getInfoString() {
      return ((FlightAntiKick.Mode)this.mode.get()).toString();
   }

   public static enum Mode {
      PhysicalSink,
      PacketDive,
      VerticalGravity;
   }
}
