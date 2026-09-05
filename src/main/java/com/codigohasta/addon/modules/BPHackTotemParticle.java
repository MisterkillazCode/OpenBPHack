package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.events.TotemParticleEvent;
import com.codigohasta.addon.utils.alien.AlienColorUtil;
import java.awt.Color;
import java.util.Random;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

public class BPHackTotemParticle extends Module {
   public static BPHackTotemParticle INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> velocityXZ = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("velocity-xz")).description("WaterSpeedNumber.")).defaultValue(100))
            .min(0)
            .max(500)
            .sliderMax(500)
            .build()
      );
   private final Setting<Integer> velocityY = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("velocity-y")).description("VerticalSpeedNumber.")).defaultValue(100))
            .min(0)
            .max(500)
            .sliderMax(500)
            .build()
      );
   private final Setting<SettingColor> color1 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("color-1"))
               .description("OneParticleColor, UseEffect."))
            .defaultValue(new SettingColor(55, 135, 255, 255))
            .build()
      );
   private final Setting<SettingColor> color2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("color-2"))
               .description("ParticleColor, UseEffect."))
            .defaultValue(new SettingColor(10, 14, 22, 255))
            .build()
      );
   private final Random random = new Random();

   public BPHackTotemParticle() {
      super(AddonTemplate.CATEGORY, "BPHackTotemParticle", "Customises the totem pop particle effect: velocity and colours.");
      INSTANCE = this;
   }

   @EventHandler
   private void onTotemParticle(TotemParticleEvent event) {
      event.cancel();
      event.velocityX = event.velocityX * (((Integer)this.velocityXZ.get()).intValue() / 100.0);
      event.velocityZ = event.velocityZ * (((Integer)this.velocityXZ.get()).intValue() / 100.0);
      event.velocityY = event.velocityY * (((Integer)this.velocityY.get()).intValue() / 100.0);
      Color c1 = new Color(
         ((SettingColor)this.color1.get()).r, ((SettingColor)this.color1.get()).g, ((SettingColor)this.color1.get()).b, ((SettingColor)this.color1.get()).a
      );
      Color c2 = new Color(
         ((SettingColor)this.color2.get()).r, ((SettingColor)this.color2.get()).g, ((SettingColor)this.color2.get()).b, ((SettingColor)this.color2.get()).a
      );
      event.color = AlienColorUtil.fadeColor(c1, c2, this.random.nextDouble());
   }
}
