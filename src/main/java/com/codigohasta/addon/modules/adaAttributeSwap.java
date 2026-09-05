package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class adaAttributeSwap extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<adaAttributeSwap.TriggerMode> triggerMode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("TriggerMode"))
                  .description("Reach: SwordSwitchSpearGetDistance; Dash: MiscSwitchSpearTriggerCharge."))
               .defaultValue(adaAttributeSwap.TriggerMode.Dash))
            .build()
      );
   private final Setting<Boolean> packetSwap = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DataPackSwitch"))
                  .description("ForceServerSendSwitchPack, IncreaseServerSuccessRate."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> stayTicks = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("HoldTick"))
                  .description("atHandinHoldTargetWeaponmanyfewTickafterSwitch(ServerSuggest 1-3)."))
               .defaultValue(2))
            .min(1)
            .sliderMax(10)
            .build()
      );
   private int originalSlot = -1;
   private int timer = 0;
   private boolean isSwapping = false;
   private boolean wasAttackPressed = false;

   public adaAttributeSwap() {
      super(
         AddonTemplate.CATEGORY,
         "adaAttributeSwap",
         "Auto-swaps item attributes; can complete spear-dash and one-tap point-set fast keydown triggers. Untested against anti-cheat."
      );
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.isSwapping) {
            this.timer++;
            if (this.timer >= (Integer)this.stayTicks.get()) {
               this.executeSwap(this.originalSlot);
               this.resetSwap();
            }
         } else {
            boolean isAttackPressed = this.mc.options.attackKey.isPressed();
            if (isAttackPressed && !this.wasAttackPressed) {
               ItemStack mainHand = this.mc.player.getMainHandStack();
               if (this.isCorrectTrigger(mainHand)) {
                  FindItemResult target = InvUtils.findInHotbar(this::isTargetItem);
                  if (target.found() && target.slot() != this.getCurrentSlot()) {
                     this.originalSlot = this.getCurrentSlot();
                     this.executeSwap(target.slot());
                     this.isSwapping = true;
                     this.timer = 0;
                  }
               }
            }

            this.wasAttackPressed = isAttackPressed;
         }
      }
   }

   private void executeSwap(int slot) {
      if (slot != -1) {
         ((InventoryAccessor)this.mc.player.getInventory()).setSelectedSlot(slot);
         if ((Boolean)this.packetSwap.get() && this.mc.getNetworkHandler() != null) {
            this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }
      }
   }

   private int getCurrentSlot() {
      return ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
   }

   private void resetSwap() {
      this.isSwapping = false;
      this.originalSlot = -1;
      this.timer = 0;
   }

   private boolean isCorrectTrigger(ItemStack stack) {
      String itemStr = stack.getItem().toString().toLowerCase();

      return switch ((adaAttributeSwap.TriggerMode)this.triggerMode.get()) {
         case Reach -> itemStr.contains("sword") || itemStr.contains("_axe");
         case Dash -> !itemStr.contains("sword")
            && !itemStr.contains("_axe")
            && !itemStr.contains("mace")
            && !itemStr.contains("spear")
            && !itemStr.contains("trident");
         case Any -> true;
      };
   }

   private boolean isTargetItem(ItemStack stack) {
      String itemStr = stack.getItem().toString().toLowerCase();
      return itemStr.contains("spear") || itemStr.contains("mace") || itemStr.contains("trident");
   }

   public void onDeactivate() {
      if (this.isSwapping && this.originalSlot != -1) {
         this.executeSwap(this.originalSlot);
      }

      this.resetSwap();
   }

   public static enum TriggerMode {
      Reach,
      Dash,
      Any;
   }
}
