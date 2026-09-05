package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.List;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class AutoSmithing extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgUpgrade = this.settings.createGroup("RiseMode (Gold)");
   private final SettingGroup sgTrim = this.settings.createGroup("Mode (Trim)");
   private final Setting<AutoSmithing.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Mode")).description("SelectareProceedEquipmentRisestillAdd."))
               .defaultValue(AutoSmithing.Mode.Upgrade))
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("OperationDelay"))
               .defaultValue(3))
            .min(1)
            .sliderMax(10)
            .build()
      );
   private final Setting<Boolean> autoDrop = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("AutoComplete"))
               .defaultValue(false))
            .build()
      );
   private final Setting<List<Item>> upgradeTargets = this.sgUpgrade
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                     .name("RiseTarget"))
                  .description("SelectwantRiseForGold'sEquipment."))
               .defaultValue(
                  new Item[]{
                     Items.DIAMOND_SWORD,
                     Items.DIAMOND_AXE,
                     Items.DIAMOND_PICKAXE,
                     Items.DIAMOND_SHOVEL,
                     Items.DIAMOND_HOE,
                     Items.DIAMOND_HELMET,
                     Items.DIAMOND_CHESTPLATE,
                     Items.DIAMOND_LEGGINGS,
                     Items.DIAMOND_BOOTS
                  }
               )
               .visible(() -> this.mode.get() == AutoSmithing.Mode.Upgrade))
            .filter(this::isDiamondGear)
            .build()
      );
   private final Setting<List<Item>> trimTargets = this.sgTrim
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                     .name("Target"))
                  .description("SelectwantAdd's."))
               .defaultValue(new Item[]{Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE})
               .visible(() -> this.mode.get() == AutoSmithing.Mode.Trim))
            .filter(
               item -> item.toString().contains("helmet")
                  || item.toString().contains("chestplate")
                  || item.toString().contains("leggings")
                  || item.toString().contains("boots")
            )
            .build()
      );
   private final Setting<List<Item>> trimTemplates = this.sgTrim
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                     .name("Use's"))
                  .description("OnlyUseSelectin's (Template)."))
               .defaultValue(
                  new Item[]{
                     Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,
                     Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,
                     Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE,
                     Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE,
                     Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
                     Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
                     Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
                     Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE,
                     Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
                     Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
                     Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
                     Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
                     Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE,
                     Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
                     Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,
                     Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE
                  }
               )
               .visible(() -> this.mode.get() == AutoSmithing.Mode.Trim))
            .filter(item -> item.toString().contains("template") && item != Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
            .build()
      );
   private final Setting<List<Item>> trimMaterials = this.sgTrim
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                     .name("Use's"))
                  .description("OnlyUseSelectin's (Color)."))
               .defaultValue(
                  new Item[]{
                     Items.IRON_INGOT,
                     Items.COPPER_INGOT,
                     Items.GOLD_INGOT,
                     Items.LAPIS_LAZULI,
                     Items.EMERALD,
                     Items.DIAMOND,
                     Items.NETHERITE_INGOT,
                     Items.REDSTONE,
                     Items.AMETHYST_SHARD,
                     Items.QUARTZ
                  }
               )
               .visible(() -> this.mode.get() == AutoSmithing.Mode.Trim))
            .build()
      );
   private int timer = 0;

   public AutoSmithing() {
      super(AddonTemplate.SC_CATEGORY, "AutoSmithing", "Auto-smiths and upgrades gold equipment (netherite upgrade helper).");
   }

   public void onActivate() {
      this.timer = 0;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.mc.player.currentScreenHandler instanceof SmithingScreenHandler handler) {
            if (this.timer > 0) {
               this.timer--;
            } else if (handler.getSlot(3).hasStack()) {
               if ((Boolean)this.autoDrop.get()) {
                  this.mc.interactionManager.clickSlot(handler.syncId, 3, 1, SlotActionType.THROW, this.mc.player);
               } else {
                  this.mc.interactionManager.clickSlot(handler.syncId, 3, 0, SlotActionType.QUICK_MOVE, this.mc.player);
               }

               this.timer = (Integer)this.delay.get();
            } else {
               if (this.mode.get() == AutoSmithing.Mode.Upgrade) {
                  this.handleUpgrade(handler);
               } else {
                  this.handleTrim(handler);
               }
            }
         }
      }
   }

   private void handleUpgrade(SmithingScreenHandler handler) {
      if (!handler.getSlot(0).hasStack()) {
         int slot = this.findItem(item -> item == Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
         if (slot != -1) {
            this.moveToSlot(slot, 0);
            this.timer = (Integer)this.delay.get();
            return;
         }
      }

      if (!handler.getSlot(2).hasStack()) {
         int slot = this.findItem(item -> item == Items.NETHERITE_INGOT);
         if (slot != -1) {
            this.moveToSlot(slot, 2);
            this.timer = (Integer)this.delay.get();
            return;
         }
      }

      if (!handler.getSlot(1).hasStack()) {
         int slot = this.findItem(item -> ((List)this.upgradeTargets.get()).contains(item));
         if (slot != -1) {
            this.moveToSlot(slot, 1);
            this.timer = (Integer)this.delay.get();
         }
      }
   }

   private void handleTrim(SmithingScreenHandler handler) {
      if (!handler.getSlot(0).hasStack()) {
         int slot = this.findItem(item -> ((List)this.trimTemplates.get()).contains(item));
         if (slot != -1) {
            this.moveToSlot(slot, 0);
            this.timer = (Integer)this.delay.get();
            return;
         }
      }

      if (!handler.getSlot(2).hasStack()) {
         int slot = this.findItem(item -> ((List)this.trimMaterials.get()).contains(item));
         if (slot != -1) {
            this.moveToSlot(slot, 2);
            this.timer = (Integer)this.delay.get();
            return;
         }
      }

      if (!handler.getSlot(1).hasStack()) {
         int slot = this.findItem(item -> ((List)this.trimTargets.get()).contains(item));
         if (slot != -1) {
            this.moveToSlot(slot, 1);
            this.timer = (Integer)this.delay.get();
         }
      }
   }

   private int findItem(Predicate<Item> predicate) {
      ScreenHandler handler = this.mc.player.currentScreenHandler;

      for (int i = 4; i < handler.slots.size(); i++) {
         if (handler.getSlot(i).hasStack() && predicate.test(handler.getSlot(i).getStack().getItem())) {
            return i;
         }
      }

      return -1;
   }

   private void moveToSlot(int sourceSlot, int targetSlot) {
      InvUtils.move().fromId(sourceSlot).toId(targetSlot);
   }

   private boolean isDiamondGear(Item item) {
      String name = item.toString();
      return name.contains("diamond_");
   }

   public static enum Mode {
      Upgrade,
      Trim;
   }
}
