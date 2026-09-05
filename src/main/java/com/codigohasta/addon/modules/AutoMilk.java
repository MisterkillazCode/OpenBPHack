package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoMilk extends Module {
   public static AutoMilk INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Delay")).description("Minimum interval between two drinks, in ticks.")).defaultValue(20))
            .min(0)
            .sliderRange(0, 100)
            .build()
      );
   private final Setting<Boolean> keepBuffs = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("KeepBuffs"))
                  .description("Do not drink while you still have buffs, so strength / speed are not wiped."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> onlyDebuffs = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("OnlyHarmful"))
                  .description("Only drink when a harmful effect is present."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> hotbarOnly = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("HotbarOnly"))
                  .description("Only use milk buckets from the hotbar."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> chatWarning = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("ChatWarning"))
                  .description("Print a chat message when drinking."))
               .defaultValue(false))
            .build()
      );
   private int timer = 0;

   public AutoMilk() {
      super(AddonTemplate.SC_CATEGORY, "AutoMilk", "Drinks milk to clear negative effects when afflicted. Can keep your buffs intact.");
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
         } else if (this.shouldDrink()) {
            FindItemResult milk = this.hotbarOnly.get() ? InvUtils.findInHotbar(new Item[]{Items.MILK_BUCKET}) : InvUtils.find(new Item[]{Items.MILK_BUCKET});
            if (milk.found()) {
               if (milk.isOffhand()) {
                  this.mc.interactionManager.interactItem(this.mc.player, Hand.OFF_HAND);
                  this.mc.player.swingHand(Hand.OFF_HAND);
               } else {
                  InvUtils.swap(milk.slot(), true);
                  this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
                  this.mc.player.swingHand(Hand.MAIN_HAND);
                  InvUtils.swapBack();
               }

               if ((Boolean)this.chatWarning.get()) {
                  this.info("清除了负面效果。", new Object[0]);
               }

               this.timer = (Integer)this.delay.get();
            }
         }
      }
   }

   private boolean shouldDrink() {
      boolean hasHarmful = false;
      boolean hasBuff = false;

      for (StatusEffectInstance instance : this.mc.player.getStatusEffects()) {
         if (((StatusEffect)instance.getEffectType().value()).isBeneficial()) {
            hasBuff = true;
         } else {
            hasHarmful = true;
         }
      }

      if ((Boolean)this.onlyDebuffs.get() && !hasHarmful) {
         return false;
      } else if ((Boolean)this.keepBuffs.get() && hasBuff) {
         return false;
      } else {
         return this.onlyDebuffs.get() ? hasHarmful : hasHarmful || hasBuff;
      }
   }

   public List<String> getHarmfulEffects() {
      List<String> out = new ArrayList<>();
      if (this.mc.player == null) {
         return out;
      } else {
         for (StatusEffectInstance instance : this.mc.player.getStatusEffects()) {
            if (!((StatusEffect)instance.getEffectType().value()).isBeneficial()) {
               out.add(((StatusEffect)instance.getEffectType().value()).getTranslationKey());
            }
         }

         return out;
      }
   }
}
