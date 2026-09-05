package com.codigohasta.addon.modules.villager;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

public class EnchantSortConfig {
   private final Map<RegistryKey<Enchantment>, Block> sortingMap = new HashMap<>();
   private Block defaultFallbackBox = Blocks.SHULKER_BOX;

   public EnchantSortConfig() {
      this.sortingMap.put(Enchantments.MENDING, Blocks.LIGHT_BLUE_SHULKER_BOX);
      this.sortingMap.put(Enchantments.UNBREAKING, Blocks.BLACK_SHULKER_BOX);
      this.sortingMap.put(Enchantments.SHARPNESS, Blocks.RED_SHULKER_BOX);
      this.sortingMap.put(Enchantments.EFFICIENCY, Blocks.YELLOW_SHULKER_BOX);
      this.sortingMap.put(Enchantments.PROTECTION, Blocks.WHITE_SHULKER_BOX);
      this.sortingMap.put(Enchantments.FORTUNE, Blocks.GREEN_SHULKER_BOX);
      this.sortingMap.put(Enchantments.SILK_TOUCH, Blocks.GRAY_SHULKER_BOX);
   }

   public void addRule(RegistryKey<Enchantment> enchantment, Block shulkerColor) {
      this.sortingMap.put(enchantment, shulkerColor);
   }

   public void clearRules() {
      this.sortingMap.clear();
   }

   public void setDefaultFallbackBox(Block defaultFallbackBox) {
      this.defaultFallbackBox = defaultFallbackBox;
   }

   public Block getTargetBoxType(ItemStack bookStack) {
      if (bookStack != null && !bookStack.isEmpty() && bookStack.getItem().toString().contains("enchanted_book")) {
         ItemEnchantmentsComponent enchants = (ItemEnchantmentsComponent)bookStack.get(DataComponentTypes.STORED_ENCHANTMENTS);
         if (enchants != null && !enchants.isEmpty()) {
            for (Entry<RegistryEntry<Enchantment>> entry : enchants.getEnchantmentEntries()) {
               RegistryKey<Enchantment> key = (RegistryKey<Enchantment>)((RegistryEntry)entry.getKey()).getKey().orElse(null);
               if (key != null && this.sortingMap.containsKey(key)) {
                  return this.sortingMap.get(key);
               }
            }

            return this.defaultFallbackBox;
         } else {
            return this.defaultFallbackBox;
         }
      } else {
         return this.defaultFallbackBox;
      }
   }

   public RegistryKey<Enchantment> getMainEnchantment(ItemStack bookStack) {
      if (bookStack != null && !bookStack.isEmpty() && bookStack.getItem().toString().contains("enchanted_book")) {
         ItemEnchantmentsComponent enchants = (ItemEnchantmentsComponent)bookStack.get(DataComponentTypes.STORED_ENCHANTMENTS);
         if (enchants != null && !enchants.isEmpty()) {
            Iterator var3 = enchants.getEnchantmentEntries().iterator();
            if (var3.hasNext()) {
               Entry<RegistryEntry<Enchantment>> entry = (Entry<RegistryEntry<Enchantment>>)var3.next();
               return (RegistryKey<Enchantment>)((RegistryEntry)entry.getKey()).getKey().orElse(null);
            } else {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public int getEnchantmentLevel(ItemStack bookStack, RegistryKey<Enchantment> targetKey) {
      if (bookStack != null && !bookStack.isEmpty() && targetKey != null) {
         ItemEnchantmentsComponent enchants = (ItemEnchantmentsComponent)bookStack.get(DataComponentTypes.STORED_ENCHANTMENTS);
         if (enchants == null) {
            return 0;
         } else {
            for (Entry<RegistryEntry<Enchantment>> entry : enchants.getEnchantmentEntries()) {
               RegistryKey<Enchantment> key = (RegistryKey<Enchantment>)((RegistryEntry)entry.getKey()).getKey().orElse(null);
               if (targetKey.equals(key)) {
                  return entry.getIntValue();
               }
            }

            return 0;
         }
      } else {
         return 0;
      }
   }
}
