package com.codigohasta.addon.modules.villager;

import com.codigohasta.addon.utils.heutil.HeBlockUtils;
import com.codigohasta.addon.utils.heutil.HeInvUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ShulkerManager {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private BlockPos targetBoxPos;
   private Item targetBoxItem;
   private int waitTimer = 0;
   private boolean isBreaking = false;
   private int searchAttempts = 0;

   public void initReplacement(BlockPos pos, Item boxItem) {
      this.targetBoxPos = pos;
      this.targetBoxItem = boxItem;
      this.waitTimer = 0;
      this.isBreaking = false;
      this.searchAttempts = 0;
   }

   public boolean tickBreakFullBox() {
      if (this.waitTimer > 0) {
         this.waitTimer--;
         return false;
      } else {
         BlockState state = mc.world.getBlockState(this.targetBoxPos);
         if (state.isAir()) {
            if (this.isBreaking) {
               this.isBreaking = false;
               this.waitTimer = 15;
               return false;
            } else {
               return true;
            }
         } else {
            FindItemResult pickaxe = InvUtils.findInHotbar(itemStack -> itemStack.getItem().toString().contains("pickaxe"));
            if (pickaxe.found()) {
               HeInvUtils.swapToSlot(pickaxe.slot());
            }

            Rotations.rotate(Rotations.getYaw(this.targetBoxPos), Rotations.getPitch(this.targetBoxPos));
            mc.interactionManager.updateBlockBreakingProgress(this.targetBoxPos, Direction.UP);
            mc.player.swingHand(Hand.MAIN_HAND);
            this.isBreaking = true;
            return false;
         }
      }
   }

   public boolean tickDumpFullBox(BlockPos dumpChestPos) {
      if (this.waitTimer > 0) {
         this.waitTimer--;
         return false;
      } else {
         ScreenHandler handler = mc.player.currentScreenHandler;
         if (handler instanceof PlayerScreenHandler) {
            HeBlockUtils.open(dumpChestPos);
            this.waitTimer = 10;
            return false;
         } else {
            FindItemResult fullBox = InvUtils.find(itemStack -> isNonEmptyShulker(itemStack));
            if (fullBox.found()) {
               InvUtils.shiftClick().slot(fullBox.slot());
               this.waitTimer = 5;
               return false;
            } else {
               HeInvUtils.closeCurScreen();
               this.waitTimer = 10;
               return true;
            }
         }
      }
   }

   public boolean tickTakeEmptyBox(BlockPos emptyBoxChestPos) {
      if (this.waitTimer > 0) {
         this.waitTimer--;
         return false;
      } else {
         ScreenHandler handler = mc.player.currentScreenHandler;
         if (handler instanceof PlayerScreenHandler) {
            HeBlockUtils.open(emptyBoxChestPos);
            this.waitTimer = 10;
            this.searchAttempts = 0;
            return false;
         } else if (InvUtils.find(itemStack -> isMatchColorEmptyShulker(itemStack, this.targetBoxItem)).found()) {
            HeInvUtils.closeCurScreen();
            this.waitTimer = 10;
            return true;
         } else {
            boolean foundInContainer = false;

            for (int i = 0; i < handler.slots.size(); i++) {
               Slot slot = handler.getSlot(i);
               if (slot.inventory != mc.player.getInventory()) {
                  ItemStack stack = slot.getStack();
                  if (isMatchColorEmptyShulker(stack, this.targetBoxItem)) {
                     InvUtils.shiftClick().slotId(i);
                     this.waitTimer = 5;
                     foundInContainer = true;
                     return false;
                  }
               }
            }

            if (!foundInContainer) {
               this.searchAttempts++;
               if (this.searchAttempts > 3) {
                  ChatUtils.error("HeavyWarning: Airgive(Device)insidenototo'sAir! Player.", new Object[0]);
                  HeInvUtils.closeCurScreen();
                  this.waitTimer = 20;
                  return true;
               }

               this.waitTimer = 10;
            }

            return false;
         }
      }
   }

   public boolean tickPlaceNewBox() {
      if (this.waitTimer > 0) {
         this.waitTimer--;
         return false;
      } else {
         FindItemResult emptyBox = InvUtils.find(itemStack -> isMatchColorEmptyShulker(itemStack, this.targetBoxItem));
         if (!emptyBox.found()) {
            return false;
         } else if (!HeInvUtils.isHotbar(emptyBox.slot())) {
            int mainSlot = HeInvUtils.getMainSlot();
            InvUtils.move().from(emptyBox.slot()).toHotbar(mainSlot);
            this.waitTimer = 5;
            return false;
         } else {
            boolean placed = HeBlockUtils.place(this.targetBoxPos, emptyBox.slot(), true, Direction.DOWN, this.targetBoxPos.toCenterPos());
            if (placed) {
               this.waitTimer = 10;
               return true;
            } else {
               return false;
            }
         }
      }
   }

   public static boolean isNonEmptyShulker(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         if (!stack.getItem().toString().contains("shulker_box")) {
            return false;
         } else {
            ContainerComponent container = (ContainerComponent)stack.get(DataComponentTypes.CONTAINER);
            return container == null ? false : container.iterateNonEmpty().iterator().hasNext();
         }
      } else {
         return false;
      }
   }

   public static boolean isMatchColorEmptyShulker(ItemStack stack, Item targetBoxItem) {
      if (stack == null || stack.isEmpty()) {
         return false;
      } else if (!stack.isOf(targetBoxItem)) {
         return false;
      } else {
         ContainerComponent container = (ContainerComponent)stack.get(DataComponentTypes.CONTAINER);
         return container == null ? true : !container.iterateNonEmpty().iterator().hasNext();
      }
   }
}
