package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoPearl extends Module {
   public static AutoPearl INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> healthThreshold = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("HealthThreshold")).description("Trigger below this much health; 20 = full health.")).defaultValue(8))
            .min(1)
            .sliderRange(1, 20)
            .build()
      );
   private final Setting<AutoPearl.Direction> direction = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Direction"))
                  .description("Which way to throw: backwards / current view / straight up."))
               .defaultValue(AutoPearl.Direction.Backwards))
            .build()
      );
   private final Setting<Double> pitch = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("Pitch"))
               .description("Look angle to throw at; negative aims upward."))
            .defaultValue(-20.0)
            .min(-90.0)
            .sliderRange(-90.0, 90.0)
            .build()
      );
   private final Setting<Integer> cooldown = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Cooldown")).description("Cooldown between two throws, in ticks.")).defaultValue(60))
            .min(0)
            .sliderRange(0, 200)
            .build()
      );
   private final Setting<Boolean> onlyHotbar = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("HotbarOnly"))
                  .description("Only use pearls from the hotbar."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> swapBack = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("SwapBack"))
                  .description("Swap back to your previous item afterwards."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> pauseInLiquid = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("PauseInLiquid"))
                  .description("Do not trigger while in water or lava."))
               .defaultValue(true))
            .build()
      );
   private int timer = 0;

   public AutoPearl() {
      super(AddonTemplate.SC_CATEGORY, "AutoPearl", "Throws an ender pearl to escape at low health. Direction can be backwards, current view, or straight up.");
      INSTANCE = this;
   }

   public void onActivate() {
      this.timer = 0;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.timer > 0) {
            this.timer--;
         } else if (!(this.mc.player.getHealth() > ((Integer)this.healthThreshold.get()).intValue())) {
            if (!(Boolean)this.pauseInLiquid.get() || !this.mc.player.isTouchingWater() && !this.mc.player.isInLava()) {
               FindItemResult pearl = this.onlyHotbar.get() ? InvUtils.findInHotbar(new Item[]{Items.ENDER_PEARL}) : InvUtils.find(new Item[]{Items.ENDER_PEARL});
               if (pearl.found()) {
                  float yaw;
                  float throwPitch;
                  switch ((AutoPearl.Direction)this.direction.get()) {
                     case Backwards:
                        yaw = this.mc.player.getYaw() + 180.0F;
                        throwPitch = (float)((Double)this.pitch.get()).doubleValue();
                        break;
                     case Up:
                        yaw = this.mc.player.getYaw();
                        throwPitch = -90.0F;
                        break;
                     default:
                        yaw = this.mc.player.getYaw();
                        throwPitch = (float)((Double)this.pitch.get()).doubleValue();
                  }

                  if (!pearl.isOffhand()) {
                     InvUtils.swap(pearl.slot(), (Boolean)this.swapBack.get());
                  }

                  Hand hand = pearl.isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND;
                  Rotations.rotate(yaw, throwPitch, 100, () -> {
                     this.mc.interactionManager.interactItem(this.mc.player, hand);
                     this.mc.player.swingHand(hand);
                     if ((Boolean)this.swapBack.get()) {
                        InvUtils.swapBack();
                     }
                  });
                  this.timer = (Integer)this.cooldown.get();
               }
            }
         }
      }
   }

   public static enum Direction {
      Backwards,
      Current,
      Up;
   }
}
