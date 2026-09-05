package com.codigohasta.addon.utils.heutil;

import com.codigohasta.addon.mixin.InventoryAccessor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

public class HeInvUtils {
   public static final MinecraftClient mc = MinecraftClient.getInstance();
   public static final int MAX_SLOT = 36;

   public static boolean isShulkerBox(Item item) {
      return item == null ? false : item.toString().contains("shulker_box");
   }

   public static Set<RegistryKey<Enchantment>> getEnchantment(ItemStack stack) {
      Set<RegistryKey<Enchantment>> set = new HashSet<>();
      if (stack != null && !stack.isEmpty()) {
         ItemEnchantmentsComponent enchants = (ItemEnchantmentsComponent)stack.get(DataComponentTypes.STORED_ENCHANTMENTS);
         if (enchants == null) {
            enchants = (ItemEnchantmentsComponent)stack.get(DataComponentTypes.ENCHANTMENTS);
         }

         if (enchants != null) {
            for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
               entry.getKey().ifPresent(set::add);
            }
         }

         return set;
      } else {
         return set;
      }
   }

   public static void closeCurScreen() {
      if (mc.player != null) {
         if (!(mc.player.currentScreenHandler instanceof PlayerScreenHandler)) {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            mc.player.closeHandledScreen();
         }
      }
   }

   public static FindItemResult findAndMoveHotbar(Item item) {
      FindItemResult hotbarResult = InvUtils.findInHotbar(new Item[]{item});
      if (hotbarResult.slot() != -1) {
         return hotbarResult;
      } else {
         FindItemResult invResult = InvUtils.find(new Item[]{item});
         if (invResult.slot() == -1) {
            return null;
         } else {
            int mainSlot = getMainSlot();
            InvUtils.move().from(invResult.slot()).toHotbar(mainSlot);
            return InvUtils.findInHotbar(new Item[]{item});
         }
      }
   }

   public static FindItemResult findShulkerBoxInHotBar(Item item) {
      return InvUtils.find(itemStack -> hasItem(item, itemStack), 0, 9);
   }

   public static FindItemResult findShulkerBoxNotEmpty() {
      return InvUtils.find(itemStack -> {
         if (isShulkerBox(itemStack.getItem())) {
            ContainerComponent container = (ContainerComponent)itemStack.get(DataComponentTypes.CONTAINER);
            return container == null ? false : container.iterateNonEmpty().iterator().hasNext();
         } else {
            return false;
         }
      }, 0, 36);
   }

   public static FindItemResult findShulkerBox(Item item) {
      return InvUtils.find(itemStack -> hasItem(item, itemStack), 0, 36);
   }

   public static List<ItemStack> findAndMargeShulkerBox() {
      List<ItemStack> kitItemStackList = mc.player
         .currentScreenHandler
         .slots
         .stream()
         .<ItemStack>map(Slot::getStack)
         .filter(Objects::nonNull)
         .filter(stack -> isShulkerBox(stack.getItem()))
         .toList();
      Map<String, ItemStack> map = new LinkedHashMap<>();

      for (ItemStack kitItemStack : kitItemStackList) {
         String key = kitItemStack.getComponents().toString();
         if (map.containsKey(key)) {
            ItemStack itemStack = map.get(key);
            itemStack.setCount(itemStack.getCount() + 1);
         } else {
            map.put(key, kitItemStack.copy());
         }
      }

      List<ItemStack> itemStacks = new ArrayList<>(map.values());
      itemStacks.sort((o1, o2) -> Integer.compare(o2.getCount(), o1.getCount()));
      return itemStacks;
   }

   private static boolean hasItem(Item item, ItemStack itemStack) {
      if (isShulkerBox(itemStack.getItem())) {
         ContainerComponent container = (ContainerComponent)itemStack.get(DataComponentTypes.CONTAINER);
         if (container == null) {
            return item == Items.AIR;
         }

         if (item == Items.AIR) {
            return !container.iterateNonEmpty().iterator().hasNext();
         }

         for (ItemStack stack : container.iterateNonEmpty()) {
            if (stack.getItem() == item) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean isHotbar(int slot) {
      return slot >= 0 && slot <= 8;
   }

   public static int findBookSlot(RegistryKey<Enchantment> enchantment) {
      PlayerInventory playerInventory = mc.player.getInventory();

      for (int i = 0; i < 36; i++) {
         ItemStack bookItemStack = playerInventory.getStack(i);
         if (bookItemStack != null && bookItemStack.getItem() == Items.ENCHANTED_BOOK) {
            Set<RegistryKey<Enchantment>> bookEnchantMentSet = getEnchantment(bookItemStack);
            if (bookEnchantMentSet.contains(enchantment)) {
               return i;
            }
         }
      }

      return -1;
   }

   @SafeVarargs
   public static int findBookSlot(RegistryKey<Enchantment>... enchantmentArr) {
      PlayerInventory playerInventory = mc.player.getInventory();

      for (int i = 0; i < 36; i++) {
         ItemStack bookItemStack = playerInventory.getStack(i);
         if (bookItemStack != null && bookItemStack.getItem() == Items.ENCHANTED_BOOK) {
            Set<RegistryKey<Enchantment>> bookEnchantMentSet = getEnchantment(bookItemStack);
            boolean exist = true;

            for (RegistryKey<Enchantment> enchantment : enchantmentArr) {
               if (!bookEnchantMentSet.contains(enchantment)) {
                  exist = false;
                  break;
               }
            }

            if (exist) {
               return i;
            }
         }
      }

      return -1;
   }

   public static int findBookSlotInChest(GenericContainerScreenHandler screenHandler, RegistryKey<Enchantment> enchantment) {
      Inventory inventory = screenHandler.getInventory();

      for (int slotId = 0; slotId < inventory.size(); slotId++) {
         ItemStack bookItemStack = screenHandler.getSlot(slotId).getStack();
         if (bookItemStack != null && bookItemStack.getItem() == Items.ENCHANTED_BOOK) {
            Set<RegistryKey<Enchantment>> bookEnchantMentSet = getEnchantment(bookItemStack);
            if (bookEnchantMentSet.contains(enchantment)) {
               return slotId;
            }
         }
      }

      return -1;
   }

   public static int findEquipSlotInChest(GenericContainerScreenHandler screenHandler, Item item) {
      Inventory inventory = screenHandler.getInventory();

      for (int slotId = 0; slotId < inventory.size(); slotId++) {
         ItemStack itemStack = screenHandler.getSlot(slotId).getStack();
         if (itemStack != null && itemStack.getItem() == item) {
            Set<RegistryKey<Enchantment>> enchantments = getEnchantment(itemStack);
            if (enchantments.isEmpty()) {
               return slotId;
            }
         }
      }

      return -1;
   }

   public static int findItemSlot(Item item) {
      PlayerInventory inventory = mc.player.getInventory();

      for (int i = 0; i < 36; i++) {
         if (inventory.getStack(i).getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   public static int findFullItemSlot(Item item) {
      PlayerInventory inventory = mc.player.getInventory();

      for (int i = 0; i < 36; i++) {
         ItemStack itemStack = inventory.getStack(i);
         if (itemStack.getItem() == item && itemStack.getCount() == itemStack.getMaxCount()) {
            return i;
         }
      }

      return -1;
   }

   public static boolean findItemAndSwitch(Item item) {
      int slot = findItemSlot(item);
      return slot < 0 ? false : swapToSlot(slot);
   }

   public static int getMainSlot() {
      return ((InventoryAccessor)mc.player.getInventory()).getSelectedSlot();
   }

   public static boolean swapToSlot(int slot) {
      if (!isHotbar(slot)) {
         return false;
      } else {
         InventoryAccessor accessor = (InventoryAccessor)mc.player.getInventory();
         if (accessor.getSelectedSlot() != slot) {
            accessor.setSelectedSlot(slot);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }

         return true;
      }
   }

   public static boolean swap(Item item) {
      FindItemResult hotbarResult = InvUtils.findInHotbar(new Item[]{item});
      if (hotbarResult.slot() != -1) {
         return swapToSlot(hotbarResult.slot());
      } else {
         FindItemResult inventoryResult = InvUtils.find(new Item[]{item});
         if (inventoryResult.slot() == -1) {
            return false;
         } else {
            InvUtils.move().from(inventoryResult.slot()).toHotbar(getMainSlot());
            return true;
         }
      }
   }

   public static void swapMainHand(int fromSlot) {
      if (isHotbar(fromSlot)) {
         swapToSlot(fromSlot);
      } else {
         boolean needSwap = !InvUtils.testInMainHand(new Item[]{Items.AIR});
         InvUtils.move().from(fromSlot).toHotbar(getMainSlot());
         if (needSwap) {
            InvUtils.click().to(fromSlot);
         }
      }
   }

   public static void swap(int fromSlot, int toSlot) {
      if (isHotbar(fromSlot)) {
         swapToSlot(fromSlot);
      } else {
         boolean needSwap = !mc.player.getInventory().getStack(fromSlot).isEmpty();
         InvUtils.move().from(fromSlot).to(toSlot);
         if (needSwap) {
            InvUtils.click().to(fromSlot);
         }
      }
   }

   public static boolean isKitInMainHand() {
      return isShulkerBox(mc.player.getMainHandStack().getItem());
   }

   public static List<ItemStack> getShulkerContents(ItemStack shulkerStack) {
      DefaultedList<ItemStack> contents = DefaultedList.ofSize(27, ItemStack.EMPTY);
      if (!isShulkerBox(shulkerStack.getItem())) {
         return contents;
      } else {
         ContainerComponent container = (ContainerComponent)shulkerStack.get(DataComponentTypes.CONTAINER);
         if (container != null) {
            container.copyTo(contents);
         }

         return contents;
      }
   }
}
