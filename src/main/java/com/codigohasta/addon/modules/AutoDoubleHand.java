package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;

public class AutoDoubleHand extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("switch-delay")).description("ImageafterSwitchHandImage'sDelay (Tick), SuggestfewFor 2"))
               .defaultValue(2))
            .min(1)
            .max(10)
            .build()
      );
   private boolean wasHoldingTotem = false;
   private boolean needsSwitch = false;
   private int delayTimer = 0;

   public AutoDoubleHand() {
      super(AddonTemplate.CATEGORY, "AutoDoubleHand", "Method module: swaps to a second item right after switching (dual-wield helper).");
   }

   public void onActivate() {
      if (this.mc.player != null) {
         this.wasHoldingTotem = this.hasTotemInOffhand();
      }

      this.needsSwitch = false;
      this.delayTimer = 0;
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.player != null) {
         boolean holdingNow = this.hasTotemInOffhand();
         if (this.wasHoldingTotem && !holdingNow) {
            this.needsSwitch = true;
            this.delayTimer = (Integer)this.delay.get() + (Math.random() > 0.5 ? 1 : 0);
         }

         this.wasHoldingTotem = holdingNow;
         if (this.needsSwitch) {
            if (this.delayTimer > 0) {
               this.delayTimer--;
            } else {
               this.executeSafeSwitch();
               this.needsSwitch = false;
            }
         }
      }
   }

   private void executeSafeSwitch() {
      int slot = this.findHotbarTotem();
      if (slot != -1 && !this.mc.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
         InvUtils.swap(slot, false);
      }
   }

   private int findHotbarTotem() {
      for (int i = 0; i < 9; i++) {
         if (this.mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
            return i;
         }
      }

      return -1;
   }

   private boolean hasTotemInOffhand() {
      return this.mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
   }
}
