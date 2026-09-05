package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.KeybindSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

public class AutoFirework extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Keybind> keybind = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("keybind")).description("The key to press to use a firework.")).defaultValue(Keybind.none())).build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("delay"))
                  .description("The delay between using fireworks in ticks."))
               .defaultValue(10))
            .min(1)
            .sliderMax(40)
            .build()
      );
   private int timer = 0;

   public AutoFirework() {
      super(
         AddonTemplate.CATEGORY,
         "AutoFirework",
         "Uses fireworks from your hotbar on a keybind with a configurable delay. (Unreliable, prefer another elytra module.)"
      );
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.timer > 0) {
            this.timer--;
         }

         if (this.mc.player.isGliding()) {
            if (((Keybind)this.keybind.get()).isPressed()) {
               if (this.timer <= 0) {
                  if (this.isFirework(this.mc.player.getMainHandStack())) {
                     this.useFirework(Hand.MAIN_HAND);
                  } else if (this.isFirework(this.mc.player.getOffHandStack())) {
                     this.useFirework(Hand.OFF_HAND);
                  } else {
                     int slot = this.findFireworkSlot();
                     if (slot != -1) {
                        this.useFireworkSilent(slot);
                     }
                  }
               }
            }
         }
      }
   }

   private void useFirework(Hand hand) {
      this.mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(hand, 0, this.mc.player.getYaw(), this.mc.player.getPitch()));
      this.mc.player.swingHand(hand);
      this.timer = (Integer)this.delay.get();
   }

   private void useFireworkSilent(int slot) {
      int prevSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
      this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      this.mc
         .getNetworkHandler()
         .sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, this.mc.player.getYaw(), this.mc.player.getPitch()));
      this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
      this.timer = (Integer)this.delay.get();
   }

   private int findFireworkSlot() {
      for (int i = 0; i < 9; i++) {
         if (this.isFirework(this.mc.player.getInventory().getStack(i))) {
            return i;
         }
      }

      return -1;
   }

   private boolean isFirework(ItemStack stack) {
      return stack.getItem() instanceof FireworkRocketItem;
   }
}
