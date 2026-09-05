package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class PearlPhase extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Boolean> antiPush = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("anti-push")).description("ForbiddenClientyouOutBlock .")).defaultValue(true)).build());
   public final Setting<Boolean> removeOverlay = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("remove-overlay")).description("RemoveBlockinside'sView, letyoulikeatinsideSamelookoutsideFace."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> speed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("speed"))
               .description("BlockinsideMoveRate."))
            .defaultValue(5.0)
            .min(0.0)
            .max(20.0)
            .sliderMax(10.0)
            .build()
      );

   public PearlPhase() {
      super(AddonTemplate.CATEGORY, "PearlPhase", "Lets you phase into blocks using ender pearls.");
   }

   public void onDeactivate() {
      if (this.mc.player != null) {
         this.mc.player.noClip = false;
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.isInsideBlock()) {
            this.mc.player.noClip = true;
            this.mc.player.fallDistance = 0.0;
            this.mc.player.setOnGround(true);
            this.handleMove();
         } else {
            this.mc.player.noClip = false;
         }
      }
   }

   private void handleMove() {
      double baseSpeed = 1.0E-4;
      double finalSpeed = baseSpeed * (Double)this.speed.get();
      double n = this.mc.player.input.playerInput.forward() ? 1.0 : 0.0;
      double n2 = this.mc.player.input.playerInput.backward() ? 1.0 : 0.0;
      double n3 = this.mc.player.getYaw();
      if (n == 0.0 && n2 == 0.0) {
         this.mc.player.setVelocity(0.0, 0.0, 0.0);
      } else {
         if (n != 0.0 && n2 != 0.0) {
            n *= Math.sin(Math.PI / 4);
            n2 *= Math.cos(Math.PI / 4);
         }

         double motionX = n * finalSpeed * -Math.sin(Math.toRadians(n3)) + n2 * finalSpeed * Math.cos(Math.toRadians(n3));
         double motionZ = n * finalSpeed * Math.cos(Math.toRadians(n3)) - n2 * finalSpeed * -Math.sin(Math.toRadians(n3));
         this.mc.player.setVelocity(motionX, 0.0, motionZ);
      }
   }

   private boolean isInsideBlock() {
      return this.mc.world.getBlockCollisions(this.mc.player, this.mc.player.getBoundingBox().contract(0.001)).iterator().hasNext();
   }
}
