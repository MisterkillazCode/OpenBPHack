package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

public class KnockbackDirection extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgControls = this.settings.createGroup("Controls");
   private final Setting<Double> yawOffset = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("yaw-offset")).description("Rotation relative to your current view. 180 = Pull towards you, 90 = To the left."))
            .defaultValue(180.0)
            .min(-180.0)
            .max(180.0)
            .sliderMin(-180.0)
            .sliderMax(180.0)
            .build()
      );
   private final Setting<Double> recoilPercent = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("recoil-amount")).description("How much to twitch back towards original view (0-1)."))
            .defaultValue(0.3)
            .min(0.0)
            .max(1.0)
            .build()
      );
   private final Setting<Keybind> modifierKey = this.sgControls
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                  .name("modifier-key"))
               .defaultValue(Keybind.fromKey(342)))
            .build()
      );
   private final Setting<Double> scrollStep = this.sgControls.add(((Builder)new Builder().name("scroll-step")).defaultValue(15.0).build());
   private boolean isInternalAttack = false;

   public KnockbackDirection() {
      super(
         AddonTemplate.CATEGORY, "KnockbackDirection", "Controls knockback direction. Server anti-do may block it; logic is similar to a head-attack redirect."
      );
   }

   @EventHandler
   private void onMouseScroll(MouseScrollEvent event) {
      if (this.mc.currentScreen == null) {
         if (((Keybind)this.modifierKey.get()).isPressed()) {
            double newOffset = MathHelper.wrapDegrees((Double)this.yawOffset.get() + event.value * (Double)this.scrollStep.get());
            this.yawOffset.set(newOffset);
            this.info("Yaw Offset: " + String.format("%.1f", newOffset), new Object[0]);
            event.cancel();
         }
      }
   }

   @EventHandler(
      priority = 200
   )
   private void onAttack(AttackEntityEvent event) {
      if (!this.isInternalAttack && this.mc.player != null) {
         Entity target = event.entity;
         event.cancel();
         this.isInternalAttack = true;
         float currentYaw = this.mc.player.getYaw();
         float currentPitch = this.mc.player.getPitch();
         boolean onGround = this.mc.player.isOnGround();
         float absoluteTargetYaw = (float)MathHelper.wrapDegrees(currentYaw + (Double)this.yawOffset.get());
         this.mc.getNetworkHandler().sendPacket(new LookAndOnGround(absoluteTargetYaw, currentPitch, onGround, this.mc.player.horizontalCollision));
         this.mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, this.mc.player.isSneaking()));
         this.mc.player.swingHand(Hand.MAIN_HAND);
         float recoilYaw = (float)this.interpolateAngle(currentYaw, absoluteTargetYaw, (Double)this.recoilPercent.get());
         this.mc.getNetworkHandler().sendPacket(new LookAndOnGround(recoilYaw, currentPitch, onGround, this.mc.player.horizontalCollision));
         this.isInternalAttack = false;
      }
   }

   private double interpolateAngle(double start, double end, double factor) {
      double difference = end - start;

      while (difference < -180.0) {
         difference += 360.0;
      }

      while (difference >= 180.0) {
         difference -= 360.0;
      }

      return start + difference * factor;
   }
}
