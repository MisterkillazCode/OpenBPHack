package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;

public class AutoPotion extends Module {
   public static AutoPotion INSTANCE;
   private final SettingGroup sgHeal = this.settings.createGroup("Heal");
   private final SettingGroup sgBuff = this.settings.createGroup("Buff");
   private final Setting<Boolean> heal = this.sgHeal
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("AutoHeal")).description("Drink an instant health potion when health drops below the threshold."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> healthThreshold = this.sgHeal
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("HealthThreshold"))
                  .description("Remaining health that triggers healing; 20 = full health, 2 = one heart."))
               .defaultValue(12))
            .min(1)
            .sliderRange(1, 20)
            .build()
      );
   private final Setting<Boolean> preferRegen = this.sgHeal
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("PreferRegeneration"))
                  .description("Prefer regeneration over instant health, better for sustaining over time."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> buffStrength = this.sgBuff
      .add(((Builder)((Builder)((Builder)new Builder().name("Strength")).description("Keep a strength potion active (PvP).")).defaultValue(false)).build());
   private final Setting<Boolean> buffSpeed = this.sgBuff
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Speed")).description("Keep a speed potion active (travelling / escaping).")).defaultValue(false))
            .build()
      );
   private final Setting<Boolean> buffRegen = this.sgBuff
      .add(((Builder)((Builder)((Builder)new Builder().name("Regeneration")).description("Keep a regeneration potion active.")).defaultValue(false)).build());
   private final Setting<Boolean> buffFireRes = this.sgBuff
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("FireResistance")).description("Keep a fire resistance potion active (Nether).")).defaultValue(false))
            .build()
      );
   private final Setting<Boolean> buffNightVision = this.sgBuff
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("NightVision")).description("Keep a night vision potion active (caving).")).defaultValue(false))
            .build()
      );
   private final Setting<Boolean> buffWaterBreathing = this.sgBuff
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("WaterBreathing")).description("Keep a water breathing potion active (underwater building)."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Integer> reBuffSeconds = this.sgBuff
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("RebuffBelow"))
                  .description("Re-drink when the buff has less than this many seconds left."))
               .defaultValue(5))
            .min(0)
            .sliderRange(0, 60)
            .build()
      );
   private final Setting<Integer> useTime = this.sgBuff
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("UseTicks"))
                  .description("How many ticks to hold right click before releasing; drinking normally takes 32."))
               .defaultValue(32))
            .min(1)
            .sliderRange(5, 60)
            .build()
      );
   private final Setting<Boolean> swapBack = this.sgBuff
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("SwapBack")).description("Swap back to your previous item after drinking.")).defaultValue(true))
            .build()
      );
   private final Setting<Boolean> onlyHotbar = this.sgBuff
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("HotbarOnly")).description("Only use potions from the hotbar, do not dig through the inventory."))
               .defaultValue(false))
            .build()
      );
   private int usingTicks = 0;
   private boolean pendingSwapBack = false;
   private int swapBackDelay = 0;

   public AutoPotion() {
      super(
         AddonTemplate.SC_CATEGORY,
         "AutoPotion",
         "Drinks healing potions below a health threshold and maintains selected buffs (strength, speed, regen, fire resist, night vision, water breathing)."
      );
      INSTANCE = this;
   }

   public void onActivate() {
      this.reset();
   }

   public void onDeactivate() {
      this.reset();
   }

   private void reset() {
      this.usingTicks = 0;
      this.swapBackDelay = 0;
      this.mc.options.useKey.setPressed(false);
      if (this.mc.player != null && this.mc.player.isUsingItem()) {
         this.mc.interactionManager.stopUsingItem(this.mc.player);
      }

      if (this.pendingSwapBack && this.mc.player != null) {
         InvUtils.swapBack();
         this.pendingSwapBack = false;
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.swapBackDelay > 0) {
            if (--this.swapBackDelay == 0 && this.pendingSwapBack) {
               InvUtils.swapBack();
               this.pendingSwapBack = false;
            }
         } else if (this.usingTicks > 0) {
            if (this.mc.player.isUsingItem()) {
               this.usingTicks--;
               if (this.usingTicks == 0) {
                  this.finishUse();
               }
            } else {
               this.finishUse();
            }
         } else if (!(Boolean)this.heal.get()
            || !(this.mc.player.getHealth() <= ((Integer)this.healthThreshold.get()).intValue())
            || !this.tryDrink(this.preferRegen.get() ? this::isRegenPotion : this::isHealPotion)
               && !this.tryDrink(this.preferRegen.get() ? this::isHealPotion : this::isRegenPotion)) {
            if (!this.tryBuff((Boolean)this.buffStrength.get(), StatusEffects.STRENGTH)) {
               if (!this.tryBuff((Boolean)this.buffSpeed.get(), StatusEffects.SPEED)) {
                  if (!this.tryBuff((Boolean)this.buffRegen.get(), StatusEffects.REGENERATION)) {
                     if (!this.tryBuff((Boolean)this.buffFireRes.get(), StatusEffects.FIRE_RESISTANCE)) {
                        if (!this.tryBuff((Boolean)this.buffNightVision.get(), StatusEffects.NIGHT_VISION)) {
                           if (!this.tryBuff((Boolean)this.buffWaterBreathing.get(), StatusEffects.WATER_BREATHING)) {
                              ;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean tryBuff(boolean enabled, RegistryEntry<StatusEffect> effect) {
      if (!enabled) {
         return false;
      } else {
         return this.hasEffectLongEnough(effect) ? false : this.tryDrink(stack -> this.hasPotionEffect(stack, effect));
      }
   }

   private boolean hasEffectLongEnough(RegistryEntry<StatusEffect> effect) {
      StatusEffectInstance instance = this.mc.player.getStatusEffect(effect);
      return instance == null ? false : instance.getDuration() > (Integer)this.reBuffSeconds.get() * 20;
   }

   private boolean hasPotionEffect(ItemStack stack, RegistryEntry<StatusEffect> effect) {
      PotionContentsComponent contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
      if (contents == null) {
         return false;
      } else {
         for (StatusEffectInstance instance : contents.getEffects()) {
            if (instance.getEffectType() == effect) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean isHealPotion(ItemStack stack) {
      return stack.getItem() == Items.POTION && this.hasPotionEffect(stack, StatusEffects.INSTANT_HEALTH);
   }

   private boolean isRegenPotion(ItemStack stack) {
      return stack.getItem() == Items.POTION && this.hasPotionEffect(stack, StatusEffects.REGENERATION);
   }

   private boolean tryDrink(Predicate<ItemStack> predicate) {
      FindItemResult result = this.onlyHotbar.get() ? InvUtils.findInHotbar(predicate) : InvUtils.find(predicate);
      if (!result.found()) {
         return false;
      } else if (!result.isOffhand() && !result.isMainHand() && !result.isHotbar()) {
         return false;
      } else {
         if (result.isOffhand()) {
            this.mc.interactionManager.interactItem(this.mc.player, Hand.OFF_HAND);
            this.mc.player.swingHand(Hand.OFF_HAND);
         } else {
            if (!result.isMainHand()) {
               InvUtils.swap(result.slot(), (Boolean)this.swapBack.get());
               this.pendingSwapBack = (Boolean)this.swapBack.get();
            }

            this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
            this.mc.player.swingHand(Hand.MAIN_HAND);
         }

         this.mc.options.useKey.setPressed(true);
         this.usingTicks = (Integer)this.useTime.get();
         return true;
      }
   }

   private void finishUse() {
      this.mc.options.useKey.setPressed(false);
      if (this.mc.player != null && this.mc.player.isUsingItem()) {
         this.mc.interactionManager.stopUsingItem(this.mc.player);
      }

      this.usingTicks = 0;
      if (this.pendingSwapBack) {
         this.swapBackDelay = 2;
      }
   }
}
