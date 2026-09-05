package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;

public class ShulkerStash extends Module {
   public static ShulkerStash INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<ShulkerStash.StashMode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Mode")).description("Store = put items in; Take = pull items out; Both = store then take."))
               .defaultValue(ShulkerStash.StashMode.Store))
            .build()
      );
   private final Setting<List<Item>> items = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("Items"))
               .description("The item list. Leave empty to handle everything."))
            .defaultValue(new Item[0])
            .build()
      );
   private final Setting<Boolean> inverse = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Inverse"))
                  .description("Invert the list: handle items that are NOT on it."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> includeHotbar = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("IncludeHotbar"))
                  .description("In Store mode, also move hotbar items."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Delay"))
                  .description("Ticks to wait between two moves."))
               .defaultValue(2))
            .min(0)
            .sliderRange(0, 20)
            .build()
      );
   private final Setting<Boolean> closeAfter = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("CloseWhenDone"))
                  .description("Close the container screen automatically when finished."))
               .defaultValue(false))
            .build()
      );
   private int timer = 0;
   private boolean done = false;

   public ShulkerStash() {
      super(AddonTemplate.SC_CATEGORY, "ShulkerStash", "Moves items in or out of chests, shulker boxes and ender chests using shift-clicks.");
      INSTANCE = this;
   }

   public void onActivate() {
      this.timer = 0;
      this.done = false;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (!this.done) {
            if (this.timer > 0) {
               this.timer--;
            } else if (this.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
               if (this.mode.get() != ShulkerStash.StashMode.Take && this.store(handler)) {
                  this.timer = (Integer)this.delay.get();
               } else if (this.mode.get() != ShulkerStash.StashMode.Store && this.take(handler)) {
                  this.timer = (Integer)this.delay.get();
               } else {
                  this.done = true;
                  if ((Boolean)this.closeAfter.get()) {
                     this.mc.player.closeHandledScreen();
                  }
               }
            }
         }
      }
   }

   private boolean store(GenericContainerScreenHandler handler) {
      int start = this.includeHotbar.get() ? 0 : 9;

      for (int i = start; i <= 35; i++) {
         ItemStack stack = this.mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && this.matches(stack)) {
            InvUtils.shiftClick().slot(i);
            return true;
         }
      }

      return false;
   }

   private boolean take(GenericContainerScreenHandler handler) {
      int rows = handler.getRows();
      int containerSlots = rows * 9;

      for (int id = 0; id < containerSlots; id++) {
         ItemStack stack = handler.getSlot(id).getStack();
         if (!stack.isEmpty() && this.matches(stack)) {
            InvUtils.shiftClick().slotId(id);
            return true;
         }
      }

      return false;
   }

   private boolean matches(ItemStack stack) {
      boolean inList = ((List)this.items.get()).contains(stack.getItem());
      boolean wanted = this.inverse.get() ? !inList : inList;
      return ((List)this.items.get()).isEmpty() ? true : wanted;
   }

   public static enum StashMode {
      Store,
      Take,
      Both;
   }
}
