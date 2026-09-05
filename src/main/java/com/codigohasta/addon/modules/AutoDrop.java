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
import net.minecraft.item.Items;

public class AutoDrop extends Module {
   public static AutoDrop INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AutoDrop.ListMode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Mode")).description("Blacklist = drop the listed items; Whitelist = keep only the listed items."))
               .defaultValue(AutoDrop.ListMode.Blacklist))
            .build()
      );
   private final Setting<List<Item>> items = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("Items"))
               .description("The item list. In Blacklist mode these get dropped, in Whitelist mode only these are kept."))
            .defaultValue(
               new Item[]{
                  Items.COBBLESTONE,
                  Items.DEEPSLATE,
                  Items.DIRT,
                  Items.GRAVEL,
                  Items.ANDESITE,
                  Items.DIORITE,
                  Items.GRANITE,
                  Items.TUFF
               }
            )
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Delay"))
                  .description("How often to drop, in ticks. Keep it above 1 to avoid spam."))
               .defaultValue(4))
            .min(1)
            .sliderRange(1, 40)
            .build()
      );
   private final Setting<Boolean> pauseInScreen = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("PauseInContainer"))
                  .description("Pause while a chest, crafting table or similar screen is open."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> keepTools = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("KeepDamagedTools"))
                  .description("Do not drop tools and armour that have taken durability damage."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> keepEnchanted = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("KeepEnchanted"))
                  .description("Do not drop enchanted items."))
               .defaultValue(true))
            .build()
      );
   private int timer = 0;

   public AutoDrop() {
      super(
         AddonTemplate.SC_CATEGORY,
         "AutoDrop",
         "Automatically drops junk. Blacklist drops the listed items; whitelist keeps only the listed items. Pauses while a container screen is open."
      );
      INSTANCE = this;
   }

   public void onActivate() {
      this.timer = 0;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (!(Boolean)this.pauseInScreen.get() || this.mc.player.currentScreenHandler == this.mc.player.playerScreenHandler) {
            if (this.timer > 0) {
               this.timer--;
            } else {
               for (int i = 0; i < this.mc.player.getInventory().size(); i++) {
                  ItemStack stack = this.mc.player.getInventory().getStack(i);
                  if (!stack.isEmpty() && this.shouldDrop(stack)) {
                     InvUtils.drop().slot(i);
                     this.timer = (Integer)this.delay.get();
                     return;
                  }
               }
            }
         }
      }
   }

   private boolean shouldDrop(ItemStack stack) {
      if ((Boolean)this.keepEnchanted.get() && stack.hasEnchantments()) {
         return false;
      } else if ((Boolean)this.keepTools.get() && stack.isDamageable() && stack.getDamage() > 0) {
         return false;
      } else {
         boolean inList = ((List)this.items.get()).contains(stack.getItem());
         return this.mode.get() == AutoDrop.ListMode.Blacklist ? inList : !inList;
      }
   }

   public static enum ListMode {
      Blacklist,
      Whitelist;
   }
}
