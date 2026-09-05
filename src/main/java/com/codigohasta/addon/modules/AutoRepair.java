package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;

public class AutoRepair extends Module {
   public static AutoRepair INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AutoRepair.Trigger> trigger = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Trigger"))
                  .description("Orbs = an XP orb is nearby; Level = XP level reached the threshold; Either = whichever happens first."))
               .defaultValue(AutoRepair.Trigger.Either))
            .build()
      );
   private final Setting<Integer> minLevel = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("MinLevel"))
                  .description("XP level threshold used by the Level trigger."))
               .defaultValue(5))
            .min(0)
            .sliderRange(0, 100)
            .build()
      );
   private final Setting<Double> orbRadius = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("OrbRadius"))
               .description("How far to look for XP orbs in Orb trigger mode."))
            .defaultValue(4.0)
            .min(0.5)
            .sliderRange(0.5, 16.0)
            .build()
      );
   private final Setting<Integer> minDurabilityLoss = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("MinDurabilityLoss"))
                  .description("Minimum durability lost before bothering, so it does not swap constantly."))
               .defaultValue(20))
            .min(1)
            .sliderRange(1, 500)
            .build()
      );
   private final Setting<Boolean> requireMending = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("RequireMending"))
                  .description("Only handle items with the Mending enchantment."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> keepOffhand = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("KeepOffhand"))
                  .description("Leave the offhand alone if it already holds something (e.g. a totem)."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Delay"))
                  .description("Interval between two swaps, in ticks."))
               .defaultValue(10))
            .min(0)
            .sliderRange(0, 60)
            .build()
      );
   private int timer = 0;

   public AutoRepair() {
      super(AddonTemplate.SC_CATEGORY, "AutoRepair", "Mending hand-swap: moves your most damaged mending item to the offhand so picked-up XP repairs it first.");
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
         } else if (this.shouldTrigger()) {
            if (!(Boolean)this.keepOffhand.get() || this.mc.player.getOffHandStack().isEmpty()) {
               int slot = this.findMostDamaged();
               if (slot != -1) {
                  InvUtils.move().from(slot).toOffhand();
                  this.timer = (Integer)this.delay.get();
               }
            }
         }
      }
   }

   private boolean shouldTrigger() {
      boolean orbs = this.hasNearbyOrbs();
      boolean level = this.mc.player.experienceLevel >= (Integer)this.minLevel.get();

      return switch ((AutoRepair.Trigger)this.trigger.get()) {
         case Orbs -> orbs;
         case Level -> level;
         default -> orbs || level;
      };
   }

   private boolean hasNearbyOrbs() {
      double r = (Double)this.orbRadius.get();
      double r2 = r * r;

      for (Entity entity : this.mc.world.getEntities()) {
         if (entity instanceof ExperienceOrbEntity && entity.squaredDistanceTo(this.mc.player) <= r2) {
            return true;
         }
      }

      return false;
   }

   private int findMostDamaged() {
      int bestSlot = -1;
      int bestLoss = 0;

      for (int i = 0; i <= 35; i++) {
         ItemStack stack = this.mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.isDamageable() && (!(Boolean)this.requireMending.get() || this.hasMending(stack))) {
            int loss = stack.getDamage();
            if (loss >= (Integer)this.minDurabilityLoss.get() && loss > bestLoss) {
               bestLoss = loss;
               bestSlot = i;
            }
         }
      }

      return bestSlot;
   }

   private boolean hasMending(ItemStack stack) {
      RegistryEntry<Enchantment> mending = this.mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
      return EnchantmentHelper.getLevel(mending, stack) > 0;
   }

   public static enum Trigger {
      Orbs,
      Level,
      Either;
   }
}
