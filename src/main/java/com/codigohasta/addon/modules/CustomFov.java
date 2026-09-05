package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;

public class CustomFov extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Double> fov = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("fov")).description("CustomView.")).defaultValue(110.0).min(30.0).max(170.0).sliderMax(170.0).build());
   public final Setting<Double> itemFov = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("item-fov")).description("CustomHandHoldItemView."))
            .defaultValue(70.0)
            .min(30.0)
            .max(170.0)
            .sliderMax(170.0)
            .build()
      );

   public CustomFov() {
      super(AddonTemplate.CATEGORY, "CustomFov", "Allows custom FOV and item-in-hand view adjustments.");
   }
}
