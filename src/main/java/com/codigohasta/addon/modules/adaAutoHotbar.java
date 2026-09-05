package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class adaAutoHotbar extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgConfig = this.settings.createGroup("System");
   private final SettingGroup sgCycle = this.settings.createGroup("Setting");
   private final List<SettingGroup> presetGroups = new ArrayList<>();
   private final List<List<Setting<List<Item>>>> allPresetsSettings = new ArrayList<>();
   private final Setting<Integer> tickDelay = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("LogicDelay (Tick)")).description("ItemMoveBetween. GrimServerSuggestFor 2.")).defaultValue(2))
            .min(0)
            .sliderMax(10)
            .build()
      );
   private final Setting<adaAutoHotbar.Mode> mode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("SwitchMode"))
               .defaultValue(adaAutoHotbar.Mode.FixedMode))
            .build()
      );
   private final Setting<Boolean> fuzzyMatch = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Tolerant Match"))
                  .description("ifnoSave'sItem, UseType'sTimes."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> autoDisable = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("LogicCompleteAutoDisable"))
               .defaultValue(true))
            .build()
      );
   private final Setting<adaAutoHotbar.Preset> activePreset = this.sgConfig
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("TargetPreset"))
                  .description("currentOperation'sPresettoImage."))
               .defaultValue(adaAutoHotbar.Preset.Preset_1))
            .build()
      );
   private final Setting<Boolean> saveNow = this.sgConfig
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Savecurrentfast -> TargetPreset"))
                     .description("pointStrikeSave: younowHandup's 9 ItemSavetoupDirectionSelectin'sPresetin."))
                  .defaultValue(false))
               .onChanged(v -> {
                  if (v) {
                     this.saveHotbarToPreset();
                  }
               }))
            .build()
      );
   private final Setting<Boolean> previewNow = this.sgConfig
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("chatSkyPre -> TargetPreset"))
                     .description("atchatSkyHitOutTargetPresetinsideSavewhat."))
                  .defaultValue(false))
               .onChanged(v -> {
                  if (v) {
                     this.previewPreset();
                  }
               }))
            .build()
      );
   private final List<Setting<Boolean>> cycleEnables = new ArrayList<>();
   private int timer = 0;
   private int currentSlotIndex = 0;
   private boolean isSorting = false;

   public adaAutoHotbar() {
      super(AddonTemplate.CATEGORY, "adaAutoHotbar", "Swaps hotbar items automatically based on presets. Cannot bypass Grim; only works while standing still.");

      for (int i = 1; i <= 5; i++) {
         this.cycleEnables
            .add(
               this.sgCycle
                  .add(
                     ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                              .name("Pack: Preset" + i))
                           .defaultValue(i <= 2))
                        .build()
                  )
            );
      }

      for (int i = 0; i < 5; i++) {
         int presetNum = i + 1;
         SettingGroup pg = this.settings.createGroup("Data: Preset" + presetNum);
         this.presetGroups.add(pg);
         List<Setting<List<Item>>> currentSlots = new ArrayList<>();

         for (int slot = 1; slot <= 9; slot++) {
            currentSlots.add(
               pg.add(
                  ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                              .name("Slot" + slot))
                           .description("Preset" + presetNum + "'s" + slot + "Item."))
                        .defaultValue(new ArrayList()))
                     .build()
               )
            );
         }

         this.allPresetsSettings.add(currentSlots);
      }
   }

   public void onActivate() {
      if (this.mc.player == null) {
         this.toggle();
      } else {
         if (this.mode.get() == adaAutoHotbar.Mode.Mode) {
            this.cycleNextPreset();
         }

         this.info("atUse:" + ((adaAutoHotbar.Preset)this.activePreset.get()).name(), new Object[0]);
         this.isSorting = true;
         this.currentSlotIndex = 0;
         this.timer = 0;
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if ((Boolean)this.saveNow.get()) {
         this.saveNow.set(false);
      }

      if ((Boolean)this.previewNow.get()) {
         this.previewNow.set(false);
      }

      if (this.mc.player != null && this.isSorting) {
         if (this.timer > 0) {
            this.timer--;
         } else {
            int presetIndex = ((adaAutoHotbar.Preset)this.activePreset.get()).ordinal();
            List<Setting<List<Item>>> targetSettings = this.allPresetsSettings.get(presetIndex);

            while (this.currentSlotIndex < 9) {
               List<Item> preferredItems = (List<Item>)targetSettings.get(this.currentSlotIndex).get();
               if (!preferredItems.isEmpty() && preferredItems.get(0) != Items.AIR) {
                  Item targetItem = preferredItems.get(0);
                  if (this.isSlotCorrectExact(this.currentSlotIndex, targetItem)) {
                     this.currentSlotIndex++;
                  } else if ((Boolean)this.fuzzyMatch.get() && this.isSlotCorrectFuzzy(this.currentSlotIndex, targetItem)) {
                     this.currentSlotIndex++;
                  } else {
                     if (this.findAndMoveItem(this.currentSlotIndex, targetItem)) {
                        this.timer = (Integer)this.tickDelay.get();
                        this.currentSlotIndex++;
                        return;
                     }

                     this.currentSlotIndex++;
                  }
               } else {
                  this.currentSlotIndex++;
               }
            }

            if ((Boolean)this.autoDisable.get()) {
               this.toggle();
            } else {
               this.isSorting = false;
            }
         }
      }
   }

   private void saveHotbarToPreset() {
      if (this.mc.player != null) {
         int presetIndex = ((adaAutoHotbar.Preset)this.activePreset.get()).ordinal();
         List<Setting<List<Item>>> targetSlotSettings = this.allPresetsSettings.get(presetIndex);
         StringBuilder sb = new StringBuilder();
         sb.append("AlreadySave [").append(((adaAutoHotbar.Preset)this.activePreset.get()).name()).append("] toConfigText:");

         for (int i = 0; i < 9; i++) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            Item item = stack.getItem();
            Setting<List<Item>> setting = targetSlotSettings.get(i);
            if (item == Items.AIR) {
               setting.set(new ArrayList());
               sb.append("Air,");
            } else {
               setting.set(Collections.singletonList(item));
               sb.append(item.getName().getString()).append(", ");
            }
         }

         this.info(sb.toString(), new Object[0]);
      }
   }

   private void previewPreset() {
      int presetIndex = ((adaAutoHotbar.Preset)this.activePreset.get()).ordinal();
      List<Setting<List<Item>>> targetSlotSettings = this.allPresetsSettings.get(presetIndex);
      this.info("--- Pre" + ((adaAutoHotbar.Preset)this.activePreset.get()).name() + " ---", new Object[0]);

      for (int i = 0; i < 9; i++) {
         List<Item> items = (List<Item>)targetSlotSettings.get(i).get();
         String itemName = !items.isEmpty() && items.get(0) != Items.AIR ? items.get(0).getName().getString() : "Air";
         this.info("Slot" + (i + 1) + ": " + itemName, new Object[0]);
      }
   }

   private void cycleNextPreset() {
      int current = ((adaAutoHotbar.Preset)this.activePreset.get()).ordinal();

      for (int i = 1; i <= 5; i++) {
         int nextIndex = (current + i) % 5;
         if ((Boolean)this.cycleEnables.get(nextIndex).get()) {
            this.activePreset.set(adaAutoHotbar.Preset.values()[nextIndex]);
            return;
         }
      }
   }

   private boolean isSlotCorrectExact(int slot, Item targetItem) {
      return this.mc.player.getInventory().getStack(slot).getItem() == targetItem;
   }

   private boolean isSlotCorrectFuzzy(int slot, Item targetItem) {
      ItemStack stack = this.mc.player.getInventory().getStack(slot);
      if (stack.isEmpty()) {
         return false;
      } else {
         String currentName = stack.getItem().toString();
         String targetName = targetItem.toString();
         if (targetName.contains("enchanted_golden_apple") && currentName.contains("golden_apple")) {
            return true;
         } else {
            String targetType = this.getMediaType(targetName);
            return targetType != null && currentName.contains(targetType);
         }
      }
   }

   private boolean findAndMoveItem(int targetSlot, Item targetItem) {
      FindItemResult result = InvUtils.find(stack -> stack.getItem() == targetItem);
      if (!result.found() && (Boolean)this.fuzzyMatch.get()) {
         result = this.findFuzzyReplacement(targetItem);
      }

      if (result.found()) {
         if (result.slot() == targetSlot) {
            return false;
         } else {
            InvUtils.move().from(result.slot()).toHotbar(targetSlot);
            return true;
         }
      } else {
         return false;
      }
   }

   private FindItemResult findFuzzyReplacement(Item target) {
      String name = target.toString().toLowerCase();
      if (name.contains("enchanted_golden_apple")) {
         return InvUtils.find(s -> s.getItem().toString().contains("golden_apple") && !s.getItem().toString().contains("enchanted"));
      } else {
         String type = this.getMediaType(name);
         if (type != null) {
            String[] tiers = new String[]{"netherite", "diamond", "iron", "golden", "stone", "wooden", "chainmail"};

            for (String tier : tiers) {
               FindItemResult res = InvUtils.find(s -> s.getItem().toString().contains(tier + type));
               if (res.found()) {
                  return res;
               }
            }
         }

         return new FindItemResult(-1, 0);
      }
   }

   private String getMediaType(String name) {
      if (name.contains("_sword")) {
         return "_sword";
      } else if (name.contains("_pickaxe")) {
         return "_pickaxe";
      } else if (name.contains("_axe")) {
         return "_axe";
      } else if (name.contains("_shovel")) {
         return "_shovel";
      } else if (name.contains("_hoe")) {
         return "_hoe";
      } else if (name.contains("_helmet")) {
         return "_helmet";
      } else if (name.contains("_chestplate")) {
         return "_chestplate";
      } else if (name.contains("_leggings")) {
         return "_leggings";
      } else {
         return name.contains("_boots") ? "_boots" : null;
      }
   }

   public void onDeactivate() {
      this.isSorting = false;
   }

   public static enum Mode {
      FixedMode,
      Mode;
   }

   public static enum Preset {
      Preset_1,
      Preset_2,
      Preset_3,
      Preset_4,
      Preset_5;
   }
}
