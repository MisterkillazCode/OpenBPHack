package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.MinecraftClientAccessor;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class adaManualCrystal extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> placeDelay = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Place Delay")).description("SettingFor 1 2 canbig. For 0 by Grim CheckTest.")).defaultValue(1))
            .min(0)
            .sliderMax(4)
            .build()
      );
   private final Setting<Boolean> jitter = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("RandomMove"))
                  .description("SimPlayerpointStrike, Grim pastRate."))
               .defaultValue(true))
            .build()
      );

   public adaManualCrystal() {
      super(AddonTemplate.CATEGORY, "adaManualCrystal", "Places crystals faster; only fast water placement and you must aim manually. Works with Grim.");
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.player != null) {
         boolean holdingCrystal = this.mc.player.getMainHandStack().getItem().toString().contains("end_crystal")
            || this.mc.player.getOffHandStack().getItem().toString().contains("end_crystal");
         if (holdingCrystal) {
            int currentCooldown = ((MinecraftClientAccessor)this.mc).getItemUseCooldown();
            int targetDelay = (Integer)this.placeDelay.get();
            if ((Boolean)this.jitter.get() && targetDelay < 3 && ThreadLocalRandom.current().nextBoolean()) {
               targetDelay++;
            }

            if (currentCooldown > targetDelay && this.mc.options.useKey.isPressed()) {
               ((MinecraftClientAccessor)this.mc).setItemUseCooldown(targetDelay);
            }

            if (this.mc.options.attackKey.isPressed() && this.mc.options.useKey.isPressed() && currentCooldown > targetDelay) {
               ((MinecraftClientAccessor)this.mc).setItemUseCooldown(targetDelay);
            }
         }
      }
   }
}
