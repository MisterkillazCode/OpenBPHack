package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoFire extends Module {
   public static AutoFire INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AutoFire.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Mode")).description("How to put the fire out: place water / drink fire resistance / try both."))
               .defaultValue(AutoFire.Mode.Both))
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Delay"))
                  .description("Minimum interval between two attempts, in ticks."))
               .defaultValue(10))
            .min(0)
            .sliderRange(0, 60)
            .build()
      );
   private final Setting<Boolean> onlyBurning = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("OnlyWhenBurning"))
                  .description("Only act when actually on fire; standing next to fire without burning does nothing."))
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
   private int timer = 0;

   public AutoFire() {
      super(AddonTemplate.SC_CATEGORY, "AutoFire", "Handles being on fire: places water at your feet or drinks a fire resistance potion.");
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
         } else {
            boolean burning = this.mc.player.isOnFire();
            if (burning || !(Boolean)this.onlyBurning.get()) {
               if (burning || this.mc.world.getBlockState(this.mc.player.getBlockPos()).getBlock().toString().contains("fire")) {
                  if (!this.mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                     if (this.mode.get() != AutoFire.Mode.Potion && this.tryWater()) {
                        this.timer = (Integer)this.delay.get();
                     } else {
                        if (this.mode.get() != AutoFire.Mode.Water && this.tryPotion()) {
                           this.timer = (Integer)this.delay.get();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean tryWater() {
      FindItemResult bucket = InvUtils.findInHotbar(new Item[]{Items.WATER_BUCKET});
      if (!bucket.found()) {
         return false;
      } else {
         InvUtils.swap(bucket.slot(), (Boolean)this.swapBack.get());
         Rotations.rotate(this.mc.player.getYaw(), 90.0, 100, () -> {
            this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
            this.mc.player.swingHand(Hand.MAIN_HAND);
            if ((Boolean)this.swapBack.get()) {
               InvUtils.swapBack();
            }
         });
         return true;
      }
   }

   private boolean tryPotion() {
      FindItemResult potion = InvUtils.find(AutoFire::isFireResPotion);
      if (!potion.found()) {
         return false;
      } else {
         if (potion.isOffhand()) {
            this.mc.interactionManager.interactItem(this.mc.player, Hand.OFF_HAND);
            this.mc.player.swingHand(Hand.OFF_HAND);
         } else {
            InvUtils.swap(potion.slot(), (Boolean)this.swapBack.get());
            this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
            this.mc.player.swingHand(Hand.MAIN_HAND);
            if ((Boolean)this.swapBack.get()) {
               InvUtils.swapBack();
            }
         }

         return true;
      }
   }

   private static boolean isFireResPotion(ItemStack stack) {
      if (stack.getItem() != Items.POTION) {
         return false;
      } else {
         PotionContentsComponent contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
         if (contents == null) {
            return false;
         } else {
            for (StatusEffectInstance instance : contents.getEffects()) {
               if (instance.getEffectType() == StatusEffects.FIRE_RESISTANCE) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public static enum Mode {
      Water,
      Potion,
      Both;
   }
}
