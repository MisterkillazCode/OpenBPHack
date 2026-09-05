package com.codigohasta.addon.mixin;

import java.lang.reflect.Field;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.player.Reach;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Reach.class},
   remap = false
)
public class MixinReach {
   @Shadow
   private Setting<Double> blockReach;
   @Shadow
   private Setting<Double> entityReach;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void onInit(CallbackInfo ci) {
      this.setMax(this.blockReach, 200.0);
      this.setMax(this.entityReach, 200.0);
   }

   private void setMax(Setting<Double> setting, double max) {
      try {
         DoubleSetting ds = (DoubleSetting)setting;
         Field maxField = DoubleSetting.class.getDeclaredField("max");
         maxField.setAccessible(true);
         maxField.set(ds, max);
         Field sliderMaxField = DoubleSetting.class.getDeclaredField("sliderMax");
         sliderMaxField.setAccessible(true);
         sliderMaxField.set(ds, max);
      } catch (Exception var7) {
      }
   }
}
