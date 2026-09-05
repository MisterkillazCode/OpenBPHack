package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.leaveshack.InventoryUtil;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;

public class ScreenActions extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgSlot1 = this.settings.createGroup("fastKey1");
   private final SettingGroup sgSlot2 = this.settings.createGroup("fastKey2");
   private final SettingGroup sgSlot3 = this.settings.createGroup("fastKey3");
   private final SettingGroup sgSlot4 = this.settings.createGroup("fastKey4");
   private final Setting<Integer> range = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Reach Distance")).description("Target'smostbigReach Distance")).defaultValue(4))
            .min(1)
            .sliderRange(1, 6)
            .build()
      );
   private final Setting<Boolean> autoSearch = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AutoBlock"))
                  .description("PutTimeAutofromKeyBlock, alsoSupportMarkStartItem"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> instantBreak = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Between"))
                  .description("TimeSend STOP PackCompleteCompleteBetween"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Keybind> key1;
   private final Setting<ScreenActions.ActionType> action1 = this.sgSlot1
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("Operation"))
               .defaultValue(ScreenActions.ActionType.PLACE_BLOCK))
            .build()
      );
   private final Setting<Keybind> key2;
   private final Setting<ScreenActions.ActionType> action2;
   private final Setting<Keybind> key3;
   private final Setting<ScreenActions.ActionType> action3;
   private final Setting<Keybind> key4;
   private final Setting<ScreenActions.ActionType> action4;
   private boolean wasPressed1;
   private boolean wasPressed2;
   private boolean wasPressed3;
   private boolean wasPressed4;

   public ScreenActions() {
      super(
         AddonTemplate.CATEGORY,
         "ScreenActions",
         "Binds inventory actions (move / swap / use / drop) to hotkeys for use on servers with right-click GUI menus."
      );
      this.key1 = this.sgSlot1
         .add(
            ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                     .name("Keybind"))
                  .defaultValue(Keybind.none()))
               .build()
         );
      this.action2 = this.sgSlot2
         .add(
            ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Operation"))
                  .defaultValue(ScreenActions.ActionType.BREAK_BLOCK))
               .build()
         );
      this.key2 = this.sgSlot2
         .add(
            ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                     .name("Keybind"))
                  .defaultValue(Keybind.none()))
               .build()
         );
      this.action3 = this.sgSlot3
         .add(
            ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Operation"))
                  .defaultValue(ScreenActions.ActionType.INTERACT_BLOCK))
               .build()
         );
      this.key3 = this.sgSlot3
         .add(
            ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                     .name("Keybind"))
                  .defaultValue(Keybind.none()))
               .build()
         );
      this.action4 = this.sgSlot4
         .add(
            ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Operation"))
                  .defaultValue(ScreenActions.ActionType.USE_ITEM))
               .build()
         );
      this.key4 = this.sgSlot4
         .add(
            ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                     .name("Keybind"))
                  .defaultValue(Keybind.none()))
               .build()
         );
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.checkSlot(this.key1, this.action1, this.wasPressed1);
         this.checkSlot(this.key2, this.action2, this.wasPressed2);
         this.checkSlot(this.key3, this.action3, this.wasPressed3);
         this.checkSlot(this.key4, this.action4, this.wasPressed4);
      }
   }

   private void checkSlot(Setting<Keybind> key, Setting<ScreenActions.ActionType> action, boolean wasPressed) {
      boolean isPressed = ((Keybind)key.get()).isPressed();
      if (isPressed && !wasPressed) {
         this.executeAction((ScreenActions.ActionType)action.get());
      }

      this.setWasPressed(key, isPressed);
   }

   private void setWasPressed(Setting<Keybind> key, boolean pressed) {
      if (key == this.key1) {
         this.wasPressed1 = pressed;
      } else if (key == this.key2) {
         this.wasPressed2 = pressed;
      } else if (key == this.key3) {
         this.wasPressed3 = pressed;
      } else if (key == this.key4) {
         this.wasPressed4 = pressed;
      }
   }

   private void executeAction(ScreenActions.ActionType action) {
      try {
         switch (action) {
            case PLACE_BLOCK:
               this.placeBlock();
               break;
            case BREAK_BLOCK:
               this.breakBlock();
               break;
            case INTERACT_BLOCK:
               this.interactBlock();
               break;
            case USE_ITEM:
               this.useItem();
               break;
            case ATTACK_ENTITY:
               this.attackEntity();
         }
      } catch (Exception var3) {
         this.error("RowOperation:" + var3.getMessage(), new Object[0]);
      }
   }

   private void placeBlock() {
      if (this.mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == Type.BLOCK) {
         if (!(this.mc.player.squaredDistanceTo(hit.getBlockPos().toCenterPos()) > (Integer)this.range.get() * (Integer)this.range.get())) {
            int slot = this.getBlockSlot();
            if (slot != -1) {
               int oldSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
               if (slot != oldSlot) {
                  this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
               }

               BlockHitResult result = new BlockHitResult(hit.getPos(), hit.getSide(), hit.getBlockPos(), false);
               this.mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, 0));
               this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
               if (slot != oldSlot) {
                  this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
               }
            }
         }
      }
   }

   private void breakBlock() {
      if (this.mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == Type.BLOCK) {
         BlockPos pos = hit.getBlockPos();
         if (!(this.mc.player.squaredDistanceTo(pos.toCenterPos()) > (Integer)this.range.get() * (Integer)this.range.get())) {
            if (!this.mc.world.getBlockState(pos).isAir()) {
               this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, hit.getSide()));
               if ((Boolean)this.instantBreak.get()) {
                  this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, hit.getSide()));
               }

               this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
         }
      }
   }

   private void interactBlock() {
      if (this.mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == Type.BLOCK) {
         if (!(this.mc.player.squaredDistanceTo(hit.getBlockPos().toCenterPos()) > (Integer)this.range.get() * (Integer)this.range.get())) {
            BlockHitResult result = new BlockHitResult(hit.getPos(), hit.getSide(), hit.getBlockPos(), false);
            this.mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, 0));
            this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         }
      }
   }

   private void useItem() {
      this.mc
         .getNetworkHandler()
         .sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, this.mc.player.getYaw(), this.mc.player.getPitch()));
      this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
   }

   private void attackEntity() {
      if (this.mc.crosshairTarget instanceof EntityHitResult hit) {
         if (!(
            this.mc.player.squaredDistanceTo(hit.getEntity().getX(), hit.getEntity().getY(), hit.getEntity().getZ())
               > (Integer)this.range.get() * (Integer)this.range.get()
         )) {
            this.mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(hit.getEntity(), this.mc.player.isSneaking()));
            this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         }
      }
   }

   private int getBlockSlot() {
      int cur = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
      if (this.mc.player.getInventory().getStack(cur).getItem() instanceof BlockItem) {
         return cur;
      } else if (!(Boolean)this.autoSearch.get()) {
         return -1;
      } else {
         if (this.mc.player.currentScreenHandler != null) {
            ItemStack cursor = this.mc.player.currentScreenHandler.getCursorStack();
            if (cursor.getItem() instanceof BlockItem) {
               if (this.mc.player.getInventory().getStack(cur).isEmpty()) {
                  this.placeCursorToSlot(cur);
                  return cur;
               }

               for (int i = 0; i < 9; i++) {
                  if (this.mc.player.getInventory().getStack(i).isEmpty()) {
                     this.placeCursorToSlot(i);
                     return i;
                  }
               }

               this.placeCursorToSlot(cur);
               return cur;
            }
         }

         return InventoryUtil.findBlock();
      }
   }

   private void placeCursorToSlot(int hotbarSlot) {
      this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, hotbarSlot + 36, 0, SlotActionType.PICKUP, this.mc.player);
   }

   public static enum ActionType {
      PLACE_BLOCK,
      BREAK_BLOCK,
      INTERACT_BLOCK,
      USE_ITEM,
      ATTACK_ENTITY;
   }
}
