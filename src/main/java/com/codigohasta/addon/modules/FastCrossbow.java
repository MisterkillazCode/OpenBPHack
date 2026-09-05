package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.util.Hand;

public class FastCrossbow extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<FastCrossbow.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Mode")).description("Native: most(Recommend); Control: LogicLimit; Packet: Power."))
               .defaultValue(FastCrossbow.Mode.Native))
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("ShootStrikeDelay"))
                  .description("SendShootafter'sCooldownTick. Suggest Native For 2-3, Control/Packet For 3-5."))
               .defaultValue(3))
            .min(0)
            .max(10)
            .build()
      );
   private final Setting<Integer> tolerance = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("Load Tolerance"))
                     .description("to Control/Packet ModehaveEffect. manyPulltickDefenseStopRebound."))
                  .defaultValue(6))
               .min(0)
               .max(50)
               .visible(() -> this.mode.get() != FastCrossbow.Mode.Native))
            .build()
      );
   private int timer = 0;

   public FastCrossbow() {
      super(AddonTemplate.CATEGORY, "FastCrossbow", "Speeds up crossbow firing and reloading.");
   }

   public void onActivate() {
      this.timer = 0;
   }

   public void onDeactivate() {
      this.mc.options.useKey.setPressed(false);
      if (this.mc.player != null) {
         this.mc.interactionManager.stopUsingItem(this.mc.player);
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         Hand hand = Hand.MAIN_HAND;
         ItemStack stack = this.mc.player.getMainHandStack();
         if (!this.isWeapon(stack.getItem())) {
            stack = this.mc.player.getOffHandStack();
            hand = Hand.OFF_HAND;
         }

         if (this.isWeapon(stack.getItem())) {
            if (this.mc.options.useKey.isPressed()) {
               if (this.timer > 0) {
                  this.timer--;
                  this.mc.options.useKey.setPressed(false);
               } else {
                  boolean isCrossbow = stack.getItem() instanceof CrossbowItem;
                  if (isCrossbow) {
                     switch ((FastCrossbow.Mode)this.mode.get()) {
                        case Native:
                           this.handleCrossbowNative(stack, hand);
                           break;
                        case Control:
                           this.handleCrossbowControl(stack, hand);
                           break;
                        case Packet:
                           this.handleCrossbowPacket(stack, hand);
                     }
                  } else {
                     switch ((FastCrossbow.Mode)this.mode.get()) {
                        case Native:
                           this.handleBowNative(stack, hand);
                           break;
                        case Control:
                           this.handleBowControl(stack, hand);
                           break;
                        case Packet:
                           this.handleBowPacket(stack, hand);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isWeapon(Item item) {
      return item instanceof CrossbowItem || item instanceof BowItem;
   }

   private void handleCrossbowNative(ItemStack stack, Hand hand) {
      if (CrossbowItem.isCharged(stack)) {
         this.mc.interactionManager.interactItem(this.mc.player, hand);
         this.mc.player.swingHand(hand);
         this.timer = (Integer)this.delay.get();
      } else {
         this.mc.options.useKey.setPressed(true);
         if (!this.mc.player.isUsingItem()) {
            this.mc.interactionManager.interactItem(this.mc.player, hand);
         }
      }
   }

   private void handleCrossbowControl(ItemStack stack, Hand hand) {
      if (CrossbowItem.isCharged(stack)) {
         this.mc.interactionManager.interactItem(this.mc.player, hand);
         this.mc.player.swingHand(hand);
         this.timer = (Integer)this.delay.get();
      } else {
         this.mc.options.useKey.setPressed(true);
         if (!this.mc.player.isUsingItem()) {
            this.mc.interactionManager.interactItem(this.mc.player, hand);
         } else {
            int requiredTime = this.getPullTime(stack) + (Integer)this.tolerance.get();
            if (this.mc.player.getItemUseTime() >= requiredTime) {
               this.mc.interactionManager.stopUsingItem(this.mc.player);
            }
         }
      }
   }

   private void handleCrossbowPacket(ItemStack stack, Hand hand) {
      if (CrossbowItem.isCharged(stack)) {
         this.mc.interactionManager.interactItem(this.mc.player, hand);
         this.mc.player.swingHand(hand);
         this.timer = (Integer)this.delay.get();
      }

      if (!this.mc.player.isUsingItem()) {
         this.mc.interactionManager.interactItem(this.mc.player, hand);
         this.mc.options.useKey.setPressed(true);
      } else {
         this.mc.options.useKey.setPressed(true);
         int requiredTime = this.getPullTime(stack) + (Integer)this.tolerance.get();
         if (this.mc.player.getItemUseTime() >= requiredTime) {
            this.mc.interactionManager.stopUsingItem(this.mc.player);
         }
      }
   }

   private void handleBowNative(ItemStack stack, Hand hand) {
      this.mc.options.useKey.setPressed(true);
      if (!this.mc.player.isUsingItem()) {
         this.mc.interactionManager.interactItem(this.mc.player, hand);
      } else if (this.mc.player.getItemUseTime() >= this.getPullTime(stack)) {
         this.mc.interactionManager.stopUsingItem(this.mc.player);
         this.mc.options.useKey.setPressed(false);
         this.timer = (Integer)this.delay.get();
      }
   }

   private void handleBowControl(ItemStack stack, Hand hand) {
      if (!this.mc.player.isUsingItem()) {
         this.mc.interactionManager.interactItem(this.mc.player, hand);
         this.mc.options.useKey.setPressed(true);
      } else {
         int requiredTime = this.getPullTime(stack) + (Integer)this.tolerance.get();
         if (this.mc.player.getItemUseTime() >= requiredTime) {
            this.mc.interactionManager.stopUsingItem(this.mc.player);
            this.timer = (Integer)this.delay.get();
         }
      }
   }

   private void handleBowPacket(ItemStack stack, Hand hand) {
      if (!this.mc.player.isUsingItem()) {
         this.mc.interactionManager.interactItem(this.mc.player, hand);
         this.mc.options.useKey.setPressed(true);
      }

      int requiredTime = this.getPullTime(stack) + (Integer)this.tolerance.get();
      if (this.mc.player.isUsingItem() && this.mc.player.getItemUseTime() >= requiredTime) {
         this.mc.interactionManager.stopUsingItem(this.mc.player);
         this.timer = (Integer)this.delay.get();
      }
   }

   private int getPullTime(ItemStack stack) {
      if (stack.getItem() instanceof BowItem) {
         return 20;
      } else {
         try {
            Registry<Enchantment> registry = this.mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            Reference<Enchantment> quickChargeEntry = registry.getOrThrow(Enchantments.QUICK_CHARGE);
            int level = EnchantmentHelper.getLevel(quickChargeEntry, stack);
            return Math.max(0, 25 - 5 * level);
         } catch (Exception var5) {
            return 25;
         }
      }
   }

   public static enum Mode {
      Native,
      Control,
      Packet;
   }
}
