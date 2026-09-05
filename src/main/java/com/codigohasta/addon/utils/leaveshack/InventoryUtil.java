package com.codigohasta.addon.utils.leaveshack;

import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.modules.GlobalSetting;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;

public class InventoryUtil {
   public static final InventoryUtil INSTANCE = new InventoryUtil();
   static int lastSlot = -1;
   static int lastSelect = -1;
   static int lastPacketSlot = -1;
   private static int lastSentSlot = -1;

   private InventoryUtil() {
      MeteorClient.EVENT_BUS.subscribe(this);
   }

   @EventHandler
   public void onPacketSend(Send event) {
      if (event.packet instanceof UpdateSelectedSlotC2SPacket packet) {
         if ((Boolean)GlobalSetting.INSTANCE.noBadPackets.get() && packet.getSelectedSlot() == lastPacketSlot) {
            event.cancel();
         }

         lastPacketSlot = packet.getSelectedSlot();
      }
   }

   public static int getEquipmentLevel(PlayerEntity player, RegistryKey<Enchantment> enchantmentKey) {
      int maxLevel = 0;

      for (ItemStack stack : List.of(
         player.getEquippedStack(EquipmentSlot.FEET),
         player.getEquippedStack(EquipmentSlot.LEGS),
         player.getEquippedStack(EquipmentSlot.CHEST),
         player.getEquippedStack(EquipmentSlot.HEAD)
      )) {
         if (!stack.isEmpty()) {
            int level = getEnchantmentLevel(stack, enchantmentKey);
            if (level > maxLevel) {
               maxLevel = level;
            }
         }
      }

      return maxLevel;
   }

   public static int getEnchantmentLevel(ItemStack itemStack, RegistryKey<Enchantment> enchantment) {
      if (itemStack.isEmpty()) {
         return 0;
      } else {
         Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments = new Object2IntArrayMap();
         getEnchantments(itemStack, itemEnchantments);
         return getEnchantmentLevel(itemEnchantments, enchantment);
      }
   }

   public static int getEnchantmentLevel(Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments, RegistryKey<Enchantment> enchantment) {
      ObjectIterator var2 = Object2IntMaps.fastIterable(itemEnchantments).iterator();

      while (var2.hasNext()) {
         Entry<RegistryEntry<Enchantment>> entry = (Entry<RegistryEntry<Enchantment>>)var2.next();
         if (((RegistryEntry)entry.getKey()).matchesKey(enchantment)) {
            return entry.getIntValue();
         }
      }

      return 0;
   }

   public static void getEnchantments(ItemStack itemStack, Object2IntMap<RegistryEntry<Enchantment>> enchantments) {
      enchantments.clear();
      if (!itemStack.isEmpty()) {
         for (Entry<RegistryEntry<Enchantment>> entry : itemStack.getItem() == Items.ENCHANTED_BOOK
            ? ((ItemEnchantmentsComponent)itemStack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)).getEnchantmentEntries()
            : itemStack.getEnchantments().getEnchantmentEntries()) {
            enchantments.put((RegistryEntry)entry.getKey(), entry.getIntValue());
         }
      }
   }

   public static void inventorySwap(int slot, int selectedSlot) {
      if (slot == lastSlot) {
         switchToSlot(lastSelect);
         lastSlot = -1;
         lastSelect = -1;
      } else if (slot - 36 != selectedSlot) {
         if (slot - 36 >= 0) {
            lastSlot = slot;
            lastSelect = selectedSlot;
            switchToSlot(slot - 36);
         } else {
            MeteorClient.mc
               .interactionManager
               .clickSlot(MeteorClient.mc.player.currentScreenHandler.syncId, slot, selectedSlot, SlotActionType.SWAP, MeteorClient.mc.player);
         }
      }
   }

   public static int findItemInventorySlot(Item item) {
      for (int i = 0; i < 45; i++) {
         ItemStack stack = MeteorClient.mc.player.getInventory().getStack(i);
         if (stack.getItem() == item) {
            return i < 9 ? i + 36 : i;
         }
      }

      return -1;
   }

   public static int findBlock() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = getStackInSlot(i);
         if (stack.getItem() instanceof BlockItem
            && !BlockUtil.shiftBlocks.contains(Block.getBlockFromItem(stack.getItem()))
            && ((BlockItem)stack.getItem()).getBlock() != Blocks.COBWEB) {
            return i;
         }
      }

      return -1;
   }

   public static int findSlabBlock() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = getStackInSlot(i);
         if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof SlabBlock) {
               return i;
            }
         }
      }

      return -1;
   }

   public static ItemStack getStackInSlot(int i) {
      return MeteorClient.mc.player.getInventory().getStack(i);
   }

   public static void switchToSlot(int slot) {
      if (slot != lastSentSlot) {
         if ((Boolean)GlobalSetting.INSTANCE.clientSwitch.get()) {
            ((InventoryAccessor)MeteorClient.mc.player.getInventory()).setSelectedSlot(slot);
         }

         sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         lastSentSlot = slot;
      }
   }

   public static int findItem(Item input) {
      for (int i = 0; i < 9; i++) {
         Item item = getStackInSlot(i).getItem();
         if (Item.getRawId(item) == Item.getRawId(input)) {
            return i;
         }
      }

      return -1;
   }

   public static int findClass(Class clazz) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = getStackInSlot(i);
         if (stack != ItemStack.EMPTY) {
            if (clazz.isInstance(stack.getItem())) {
               return i;
            }

            if (stack.getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)stack.getItem()).getBlock())) {
               return i;
            }
         }
      }

      return -1;
   }

   public static int findClassInventory(Class clazz) {
      for (int i = 0; i < 45; i++) {
         ItemStack stack = getStackInSlot(i);
         if (stack != ItemStack.EMPTY) {
            if (clazz.isInstance(stack.getItem())) {
               return i < 9 ? i + 36 : i;
            }

            if (stack.getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)stack.getItem()).getBlock())) {
               return i < 9 ? i + 36 : i;
            }
         }
      }

      return -1;
   }

   public static void sendPacket(Packet<?> packet) {
      MeteorClient.mc.getNetworkHandler().sendPacket(packet);
   }

   public static int findBlock(Block block) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = getStackInSlot(i);
         if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == block) {
            return i;
         }
      }

      return -1;
   }

   public static int findBlockInventory(Block block) {
      for (int i = 0; i < 45; i++) {
         ItemStack stack = getStackInSlot(i);
         if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == block) {
            return i < 9 ? i + 36 : i;
         }
      }

      return -1;
   }

   public static enum MineSwitchMode {
      Delay,
      Silent,
      None;
   }
}
