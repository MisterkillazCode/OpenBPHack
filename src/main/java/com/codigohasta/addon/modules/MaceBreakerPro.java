package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

public class MaceBreakerPro extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> swordTrigger = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("SwordTrigger")).description("HandHoldSwordAttackTimeTrigger")).defaultValue(true)).build());
   private final Setting<Boolean> axeTrigger = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Trigger")).description("HandHoldAttackTimeTrigger")).defaultValue(true)).build());
   private final Setting<Boolean> maceTrigger = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Trigger")).description("HandHoldHeavyAttackTimeTrigger")).defaultValue(true)).build());
   private final Setting<Boolean> onlyOnShield = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("TimeTrigger")).description("enemynoTimenotRow, DefenseStopDurability.")).defaultValue(true)).build()
      );
   private final Setting<Boolean> legitMode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("LegitMode (Grim)")).description("ForceClientThisGroundSlot, past Grim 'sCheckTest."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> autoReturn = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("AttackafterSwitch")).description("CompleteCompleteafterSwitchmost'sWeapon.")).defaultValue(true))
            .build()
      );

   public MaceBreakerPro() {
      super(
         AddonTemplate.CATEGORY,
         "MaceBreakerPro",
         "Swaps to a mace right before your attack lands for extra damage. Only a toggle helper and cannot bypass anti-cheat."
      );
   }

   @EventHandler
   private void onAttackEntity(AttackEntityEvent event) {
      if (event.entity != null && this.mc.player != null && this.mc.world != null) {
         if ((Boolean)this.onlyOnShield.get()) {
            if (!(event.entity instanceof LivingEntity target)) {
               return;
            }

            if (!target.isBlocking()) {
               return;
            }
         }

         ItemStack handStack = this.mc.player.getMainHandStack();
         String handItem = handStack.getItem().toString().toLowerCase();
         boolean holdingSword = handItem.contains("sword");
         boolean holdingAxe = handItem.contains("_axe");
         boolean holdingMace = handItem.contains("mace");
         if (!holdingSword || (Boolean)this.swordTrigger.get()) {
            if (!holdingAxe || (Boolean)this.axeTrigger.get()) {
               if (!holdingMace || (Boolean)this.maceTrigger.get()) {
                  if (holdingSword || holdingAxe || holdingMace) {
                     FindItemResult axeRes = InvUtils.findInHotbar(s -> s.getItem().toString().toLowerCase().contains("_axe"));
                     FindItemResult maceRes = InvUtils.findInHotbar(s -> s.getItem().toString().toLowerCase().contains("mace"));
                     if (axeRes.found() && maceRes.found()) {
                        int axeSlot = axeRes.slot();
                        int maceSlot = maceRes.slot();
                        int originalSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                        event.cancel();
                        this.performAttackStep(axeSlot, event.entity);
                        this.performAttackStep(maceSlot, event.entity);
                        if ((Boolean)this.autoReturn.get()) {
                           this.performSlotSwitch(originalSlot);
                        } else {
                           this.performSlotSwitch(maceSlot);
                        }

                        this.mc.player.swingHand(Hand.MAIN_HAND);
                     }
                  }
               }
            }
         }
      }
   }

   private void performAttackStep(int slot, Entity target) {
      this.performSlotSwitch(slot);
      this.mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, this.mc.player.isSneaking()));
   }

   private void performSlotSwitch(int slot) {
      if (slot != -1) {
         if (((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot() != slot) {
            this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            if ((Boolean)this.legitMode.get()) {
               ((InventoryAccessor)this.mc.player.getInventory()).setSelectedSlot(slot);
            }
         }
      }
   }
}
