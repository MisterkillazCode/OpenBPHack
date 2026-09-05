package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.utils.alien.AlienEasing;
import com.codigohasta.addon.utils.alien.AlienFadeUtils;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.Perspective;

public class CameraClip extends Module {
   public static CameraClip INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> distance = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("distance")).description("Camera distance."))
            .defaultValue(4.0)
            .min(1.0)
            .max(20.0)
            .sliderRange(1.0, 20.0)
            .build()
      );
   private final Setting<Integer> animationTime = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("animation-time"))
                  .description("Animation time in milliseconds."))
               .defaultValue(200))
            .min(0)
            .max(1000)
            .sliderMax(1000)
            .build()
      );
   private final Setting<AlienEasing> ease = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("ease"))
                  .description("Easing mode for camera distance animation."))
               .defaultValue(AlienEasing.CubicInOut))
            .build()
      );
   private final Setting<Boolean> noFront = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("no-front"))
                  .description("Disable front third-person perspective."))
               .defaultValue(false))
            .build()
      );
   private final AlienFadeUtils animation = new AlienFadeUtils(300L);
   private boolean wasFirstPerson;

   public CameraClip() {
      super(AddonTemplate.CATEGORY, "CameraClip", "Extends the third-person camera distance with a smooth animation.");
      INSTANCE = this;
   }

   public void onActivate() {
      this.animation.setLength(((Integer)this.animationTime.get()).intValue());
      this.animation.reset();
      this.wasFirstPerson = this.mc.options.getPerspective() == Perspective.FIRST_PERSON;
   }

   public void onDeactivate() {
      this.wasFirstPerson = false;
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT && (Boolean)this.noFront.get()) {
         this.mc.options.setPerspective(Perspective.FIRST_PERSON);
      }

      this.animation.setLength(((Integer)this.animationTime.get()).intValue());
      if (this.mc.options.getPerspective() == Perspective.FIRST_PERSON) {
         if (!this.wasFirstPerson) {
            this.wasFirstPerson = true;
            this.animation.reset();
         }
      } else if (this.wasFirstPerson) {
         this.wasFirstPerson = false;
         this.animation.reset();
      }
   }

   public double getDistance() {
      double quad = this.animation.ease((AlienEasing)this.ease.get());
      return this.mc.options.getPerspective() == Perspective.FIRST_PERSON
         ? (Double)this.distance.get() * (1.0 - quad)
         : 0.5 + ((Double)this.distance.get() - 0.5) * quad;
   }
}
