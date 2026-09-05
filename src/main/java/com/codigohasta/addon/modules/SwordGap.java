package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.List;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.ItemListSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class SwordGap extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<Item>> allowedFoods = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("allowed-foods")).description("Select which items are allowed to be eaten."))
            .defaultValue(new Item[]{Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE})
            .filter(item -> item.getComponents().contains(DataComponentTypes.FOOD))
            .build()
      );
   private boolean isEating = false;
   private int lastFoodSlot = -1;

   public SwordGap() {
      super(AddonTemplate.CATEGORY, "SwordGap", "Sword gap (Meteor already has it). Partially unusable.");
   }

   public void onDeactivate() {
      if (this.isEating && this.mc.player != null) {
         this.restoreOffhand();
      }

      this.isEating = false;
      this.lastFoodSlot = -1;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         boolean holdingSword = this.mc.player.getMainHandStack().getItem().toString().contains("sword");
         boolean pressingUse = this.mc.options.useKey.isPressed();
         if (holdingSword && pressingUse) {
            if (!this.isEating) {
               this.startEating();
            }
         } else if (this.isEating) {
            this.restoreOffhand();
         }
      }
   }

   private void startEating() {
      int foodSlot = this.findFoodSlot();
      if (foodSlot != -1) {
         if (((List)this.allowedFoods.get()).contains(this.mc.player.getOffHandStack().getItem())) {
            this.isEating = true;
         } else {
            this.lastFoodSlot = foodSlot;
            InvUtils.move().from(foodSlot).toOffhand();
            this.isEating = true;
         }
      }
   }

   private void restoreOffhand() {
      if (this.lastFoodSlot != -1) {
         InvUtils.move().from(this.lastFoodSlot).toOffhand();
      }

      this.isEating = false;
      this.lastFoodSlot = -1;
   }

   private int findFoodSlot() {
      Predicate<ItemStack> predicate = itemStack -> ((List)this.allowedFoods.get()).contains(itemStack.getItem());
      FindItemResult result = InvUtils.find(predicate);
      return result.found() ? result.slot() : -1;
   }
}
