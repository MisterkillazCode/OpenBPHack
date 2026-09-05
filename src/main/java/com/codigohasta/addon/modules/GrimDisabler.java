package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayDeque;
import java.util.Queue;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class GrimDisabler extends Module {
   private final Queue<CommonPingS2CPacket> queue = new ArrayDeque<>();
   private boolean flag;
   private boolean flag2;
   private Vec3d vec3d;
   private int num = 0;
   private final GrimDisabler.Stopwatch stopwatch = new GrimDisabler.Stopwatch();
   private final GrimDisabler.Stopwatch stopwatch2 = new GrimDisabler.Stopwatch();

   public GrimDisabler() {
      super(AddonTemplate.CATEGORY, "GrimDisabler", "Disables Grim anticheat movement checks via a firework/elytra keepalive-freeze.");
   }

   public void onDeactivate() {
      this.reset();
   }

   private void reset() {
      this.flag = false;
      this.flag2 = false;
      this.vec3d = null;
      this.num = 0;
      this.queue.clear();
      this.stopwatch2.setTime(-1L);
   }

   private boolean canTrigger() {
      if (this.mc.player == null) {
         return false;
      } else if (!this.stopwatch2.hasElapsed(1000L)) {
         return false;
      } else if (this.findItem(Items.FIREWORK_ROCKET) == -1) {
         return false;
      } else {
         return this.findElytra() == -1 ? false : !this.mc.player.isOnGround();
      }
   }

   private void trigger() {
      int elytraSlot = this.findElytra();
      if (elytraSlot != -1) {
         if (!this.isElytraEquipped()) {
            this.equipToChestplate(elytraSlot);
         }

         this.useFirework();
         this.stopwatch2.reset();
         this.vec3d = this.mc.player.getSyncedPos();
         this.flag2 = true;
      }
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      if (event.packet instanceof CommonPingS2CPacket ping) {
         if (this.flag && this.num <= 0) {
            this.queue.add(ping);
            event.cancel();
         }
      } else if (event.packet instanceof PlayerPositionLookS2CPacket) {
         if (!this.flag || this.num > 0) {
            return;
         }

         this.reset();
      } else if (event.packet instanceof EntitySpawnS2CPacket spawn) {
         if (!this.flag2 || this.vec3d == null) {
            return;
         }

         if (spawn.getEntityType() != EntityType.FIREWORK_ROCKET) {
            return;
         }

         if (this.vec3d.squaredDistanceTo(spawn.getX(), spawn.getY(), spawn.getZ()) >= 12.5) {
            return;
         }

         this.num = 10;
         this.flag = true;
         this.flag2 = false;
         this.stopwatch.reset();
      }
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.flag && this.vec3d != null && this.mc.player.getSyncedPos().distanceTo(this.vec3d) > 82.0) {
            this.flag = false;
         }

         if (this.stopwatch.hasElapsed(45000L)) {
            this.flag = false;
         }

         if (this.flag) {
            this.num--;
         } else {
            while (!this.queue.isEmpty()) {
               CommonPingS2CPacket p = this.queue.poll();
               if (p != null) {
                  this.mc.getNetworkHandler().onPing(p);
               }
            }
         }

         if (this.canTrigger() && !this.flag) {
            this.trigger();
         }
      }
   }

   @EventHandler
   private void onPlayerMove(PlayerMoveEvent event) {
      if (this.mc.player != null) {
         this.mc.player.getAbilities().flying = false;
         this.mc.player.getAbilities().allowFlying = false;
      }
   }

   private int findItem(Item item) {
      PlayerInventory inv = this.mc.player.getInventory();

      for (int i = 0; i < inv.size(); i++) {
         if (inv.getStack(i).isOf(item)) {
            return i;
         }
      }

      return -1;
   }

   private int findElytra() {
      PlayerInventory inv = this.mc.player.getInventory();

      for (int i = 0; i < inv.size(); i++) {
         ItemStack s = inv.getStack(i);
         if (s.isOf(Items.ELYTRA) && s.getMaxDamage() - s.getDamage() > 1) {
            return i;
         }
      }

      return -1;
   }

   private boolean isElytraEquipped() {
      return this.mc.player.getInventory().getStack(38).isOf(Items.ELYTRA);
   }

   private Hand getHandWithItem(Item item) {
      if (this.mc.player.getMainHandStack().isOf(item)) {
         return Hand.MAIN_HAND;
      } else {
         return this.mc.player.getOffHandStack().isOf(item) ? Hand.OFF_HAND : null;
      }
   }

   private void equipToChestplate(int invSlot) {
      PlayerScreenHandler sh = this.mc.player.playerScreenHandler;
      int src = this.invToScreen(invSlot);
      int chest = 7;

      try {
         this.clickSlot(sh.syncId, src, 0, SlotActionType.PICKUP);
         this.clickSlot(sh.syncId, chest, 0, SlotActionType.PICKUP);
         this.clickSlot(sh.syncId, src, 0, SlotActionType.PICKUP);
      } catch (Exception var6) {
      }
   }

   private void useFirework() {
      Hand hand = this.getHandWithItem(Items.FIREWORK_ROCKET);
      if (hand != null) {
         this.interact(hand);
      } else {
         int slot = this.findItem(Items.FIREWORK_ROCKET);
         if (slot != -1) {
            int cur = this.mc.player.getInventory().getSelectedSlot();
            PlayerScreenHandler sh = this.mc.player.playerScreenHandler;

            try {
               this.clickSlot(sh.syncId, this.invToScreen(slot), cur, SlotActionType.SWAP);
               this.interact(Hand.MAIN_HAND);
               this.clickSlot(sh.syncId, this.invToScreen(slot), cur, SlotActionType.SWAP);
            } catch (Exception var6) {
            }
         }
      }
   }

   private int invToScreen(int invSlot) {
      if (invSlot < 0) {
         return -1;
      } else if (invSlot <= 8) {
         return 36 + invSlot;
      } else {
         return invSlot <= 35 ? invSlot : invSlot - 31;
      }
   }

   private void clickSlot(int syncId, int slot, int button, SlotActionType action) {
      this.mc.interactionManager.clickSlot(syncId, slot, button, action, this.mc.player);
   }

   private void interact(Hand hand) {
      this.mc.interactionManager.interactItem(this.mc.player, hand);
   }

   private static class Stopwatch {
      private long start = System.currentTimeMillis();

      void reset() {
         this.start = System.currentTimeMillis();
      }

      void setTime(long millis) {
         this.start = System.currentTimeMillis() - millis;
      }

      boolean hasElapsed(long millis) {
         return System.currentTimeMillis() - this.start >= millis;
      }
   }
}
