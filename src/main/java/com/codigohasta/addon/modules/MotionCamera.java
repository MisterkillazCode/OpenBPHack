package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.utils.alien.AlienAnimateUtil;
import com.codigohasta.addon.utils.alien.AlienMathUtil;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class MotionCamera extends Module {
   public static MotionCamera INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Boolean> noFirstPerson = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("no-first-person")).description("atFirst PersonViewdownDisable MotionCamera.")).defaultValue(true))
            .build()
      );
   public final Setting<Double> firstPersonSpeed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("first-person-speed"))
               .description("First PersonViewdown'sSpeed."))
            .defaultValue(0.6)
            .min(0.0)
            .max(1.0)
            .sliderMax(1.0)
            .build()
      );
   public final Setting<Double> speed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("speed"))
               .description("Third PersonViewdown'sSpeed."))
            .defaultValue(0.3)
            .min(0.0)
            .max(1.0)
            .sliderMax(1.0)
            .build()
      );
   private double fakeX;
   private double fakeY;
   private double fakeZ;
   private double prevFakeX;
   private double prevFakeY;
   private double prevFakeZ;

   public MotionCamera() {
      super(AddonTemplate.CATEGORY, "MotionCamera", "Smooths camera movement with a configurable speed.");
      INSTANCE = this;
   }

   public boolean on() {
      return this.isActive() && (!(Boolean)this.noFirstPerson.get() || !this.mc.options.getPerspective().isFirstPerson());
   }

   public void onActivate() {
      if (this.mc.player != null) {
         this.fakeX = this.mc.player.getX();
         this.fakeY = this.mc.player.getY() + this.mc.player.getEyeHeight(this.mc.player.getPose());
         this.fakeZ = this.mc.player.getZ();
         this.prevFakeX = this.fakeX;
         this.prevFakeY = this.fakeY;
         this.prevFakeZ = this.fakeZ;
      }
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.player != null) {
         this.prevFakeX = this.fakeX;
         this.prevFakeY = this.fakeY;
         this.prevFakeZ = this.fakeZ;
         double spd = this.mc.options.getPerspective().isFirstPerson() ? (Double)this.firstPersonSpeed.get() : (Double)this.speed.get();
         this.fakeX = AlienAnimateUtil.animate(this.fakeX, this.mc.player.getX(), spd);
         this.fakeY = AlienAnimateUtil.animate(
            this.fakeY, this.mc.player.getY() + this.mc.player.getEyeHeight(this.mc.player.getPose()), spd
         );
         this.fakeZ = AlienAnimateUtil.animate(this.fakeZ, this.mc.player.getZ(), spd);
      }
   }

   public double getFakeX(float tickDelta) {
      return AlienMathUtil.interpolate(this.prevFakeX, this.fakeX, (double)tickDelta);
   }

   public double getFakeY(float tickDelta) {
      return AlienMathUtil.interpolate(this.prevFakeY, this.fakeY, (double)tickDelta);
   }

   public double getFakeZ(float tickDelta) {
      return AlienMathUtil.interpolate(this.prevFakeZ, this.fakeZ, (double)tickDelta);
   }
}
