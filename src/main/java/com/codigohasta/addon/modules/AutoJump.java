package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class AutoJump extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> sneakWhileJumping = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("AntiSneak")).description("Use's. atJump'sTimeAntiProceedSneakandBili. goodgoodhaha"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Integer> sneakStandDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("SneakBiliTime"))
                     .description("TimesBiliStatus'sHoldTime (ticks)."))
                  .defaultValue(5))
               .min(0)
               .sliderRange(0, 20)
               .visible(this.sneakWhileJumping::get))
            .build()
      );
   private final Setting<Integer> sneakDuration = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("SneakHoldTime"))
                     .description("TimesSneakdown'sHoldTime (ticks)."))
                  .defaultValue(1))
               .min(1)
               .sliderRange(1, 20)
               .visible(this.sneakWhileJumping::get))
            .build()
      );
   private final Setting<Integer> jumpDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("JumpDelay"))
                  .description("TimesJumpofBetween'sBetweenTime (ticks). Numberlittle, gotfast."))
               .defaultValue(10))
            .min(0)
            .sliderRange(0, 40)
            .build()
      );
   private int jumpTimer;
   private int sneakActionTimer;
   private boolean isCurrentlySneaking;

   public AutoJump() {
      super(AddonTemplate.CATEGORY, "AutoJump", "Holds jump automatically. Casual helper.");
   }

   public void onActivate() {
      this.jumpTimer = 0;
      this.sneakActionTimer = 0;
      this.isCurrentlySneaking = false;
      if (this.mc.options != null) {
         this.mc.options.sneakKey.setPressed(false);
      }
   }

   public void onDeactivate() {
      if (this.mc.options != null) {
         this.mc.options.sneakKey.setPressed(false);
         this.isCurrentlySneaking = false;
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if ((Boolean)this.sneakWhileJumping.get()) {
            if (this.sneakActionTimer > 0) {
               this.sneakActionTimer--;
            } else {
               this.isCurrentlySneaking = !this.isCurrentlySneaking;
               this.mc.options.sneakKey.setPressed(this.isCurrentlySneaking);
               this.sneakActionTimer = this.isCurrentlySneaking ? (Integer)this.sneakDuration.get() : (Integer)this.sneakStandDelay.get();
            }
         } else if (this.isCurrentlySneaking) {
            this.mc.options.sneakKey.setPressed(false);
            this.isCurrentlySneaking = false;
         }

         if (this.jumpTimer > 0) {
            this.jumpTimer--;
         } else if (this.mc.player.isOnGround()) {
            this.mc.player.jump();
            this.jumpTimer = (Integer)this.jumpDelay.get();
         }
      }
   }
}
