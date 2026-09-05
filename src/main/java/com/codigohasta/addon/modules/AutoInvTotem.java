package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.slot.SlotActionType;

public class AutoInvTotem extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> minDelay = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("min-delay-ms")).description("OperationmostlittleDelay() - Suggest150")).defaultValue(200))
            .min(50)
            .max(1000)
            .build()
      );
   private final Setting<Integer> maxDelay = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("max-delay-ms")).description("OperationmostbigDelay()")).defaultValue(350)).min(50).max(1000).build()
      );
   private final Setting<Boolean> moveFromHotbar = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("move-from-hotbar"))
                  .description("whetherfromfast"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> disableLogs = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("disable-logs"))
                  .description("DisablechatSkyHint"))
               .defaultValue(true))
            .build()
      );
   private boolean needsTotem = false;
   private long executeTime = 0L;
   private boolean hadTotemInOffhand = false;

   public AutoInvTotem() {
      super(AddonTemplate.CATEGORY, "AutoInvTotem", "Auto-swaps a totem into your offhand (image/method based).");
   }

   public void onActivate() {
      this.resetState();
   }

   public void onDeactivate() {
      this.resetState();
   }

   private void resetState() {
      this.needsTotem = false;
      this.executeTime = 0L;
      if (this.mc.player != null) {
         this.hadTotemInOffhand = this.hasTotemInOffhand();
      }
   }

   @EventHandler
   private void onGameJoined(GameJoinedEvent event) {
      this.resetState();
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      if (this.mc.player != null) {
         if (event.packet instanceof EntityStatusS2CPacket packet && packet.getStatus() == 35) {
            Entity entity = packet.getEntity(this.mc.world);
            if (entity != null && entity.getId() == this.mc.player.getId()) {
               this.needsTotem = true;
               long humanJitter = ((Integer)this.minDelay.get()).intValue()
                  + (long)(Math.random() * ((Integer)this.maxDelay.get() - (Integer)this.minDelay.get() + 1));
               this.executeTime = System.currentTimeMillis() + humanJitter;
            }
         }
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null) {
         boolean currentlyHasTotem = this.hasTotemInOffhand();
         if (this.hadTotemInOffhand && !currentlyHasTotem && !this.needsTotem) {
            this.needsTotem = true;
            long humanJitter = ((Integer)this.minDelay.get()).intValue()
               + (long)(Math.random() * ((Integer)this.maxDelay.get() - (Integer)this.minDelay.get() + 1));
            this.executeTime = System.currentTimeMillis() + humanJitter;
         }

         this.hadTotemInOffhand = currentlyHasTotem;
         if (this.needsTotem && this.mc.currentScreen instanceof InventoryScreen && System.currentTimeMillis() >= this.executeTime) {
            this.executeSafeSwap();
         }

         if (currentlyHasTotem && this.needsTotem) {
            this.needsTotem = false;
         }
      }
   }

   private void executeSafeSwap() {
      int totemSlot = this.findTotemSlot();
      if (totemSlot != -1) {
         try {
            int containerSlot = totemSlot < 9 ? totemSlot + 36 : totemSlot;
            int syncId = this.mc.player.playerScreenHandler.syncId;
            this.mc.interactionManager.clickSlot(syncId, containerSlot, 40, SlotActionType.SWAP, this.mc.player);
            if (!(Boolean)this.disableLogs.get()) {
               this.info("Safely swapped totem to offhand (Bypassed Anticheat).", new Object[0]);
            }

            this.needsTotem = false;
            this.executeTime = System.currentTimeMillis() + 500L;
         } catch (Exception var4) {
            if (!(Boolean)this.disableLogs.get()) {
               this.error("Failed to move totem.", new Object[0]);
            }
         }
      }
   }

   private int findTotemSlot() {
      for (int i = 9; i < 36; i++) {
         if (this.isTotem(this.mc.player.getInventory().getStack(i))) {
            return i;
         }
      }

      if ((Boolean)this.moveFromHotbar.get()) {
         for (int ix = 0; ix < 9; ix++) {
            if (this.isTotem(this.mc.player.getInventory().getStack(ix))) {
               return ix;
            }
         }
      }

      return -1;
   }

   private boolean hasTotemInOffhand() {
      return this.mc.player == null ? false : this.isTotem(this.mc.player.getOffHandStack());
   }

   private boolean isTotem(ItemStack stack) {
      return stack != null && !stack.isEmpty() ? stack.getItem().toString().toLowerCase().contains("totem_of_undying") : false;
   }
}
