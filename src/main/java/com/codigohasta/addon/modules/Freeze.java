package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.openmyau.ChatUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Freeze extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgStationary = this.settings.createGroup("Stationary");
   private final SettingGroup sgLook = this.settings.createGroup("Look");
   private final SettingGroup sgMace = this.settings.createGroup("Mace");
   private final Setting<Freeze.FreezeMode> mode = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("mode")).description("FreezeMode")).defaultValue(Freeze.FreezeMode.Stationary)).build());
   private final Setting<Boolean> disableOnFlag = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("disable-on-flag"))
                  .description("toServicePositionPackTimeAutoDisableModule"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> notification = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("notification"))
                  .description("by flag TimeSendchatSkyHint"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> balance = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("balance"))
                  .description("DisableTimePut'sTick(Player tick)"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> bypassNegativeTimer = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("bypass-negative-timer"))
                  .description("DisableTimeRowOneTimesItemExchangepastTimeCheckTest"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> cancelC0B = this.sgStationary
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("cancel-c0b"))
                     .description("Disappear CommonPongC2SPacket(past Grim BadPacketsR)"))
                  .defaultValue(false))
               .visible(() -> this.mode.get() == Freeze.FreezeMode.Stationary))
            .build()
      );
   private final Setting<Boolean> freezeLook = this.sgLook
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("freeze-look"))
                  .description("FreezeView()"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> freezeLookSilent = this.sgLook
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("freeze-look-silent"))
                     .description("FreezeView(ClientCan, ServiceFreeze)"))
                  .defaultValue(true))
               .visible(this.freezeLook::get))
            .build()
      );
   private final Setting<Boolean> maceMode = this.sgMace
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("mace-mode"))
                     .description("Stationary ModedownslowdownService fallDistance, TimesAttackhaveHeavyStrikeDamage"))
                  .defaultValue(true))
               .visible(() -> this.mode.get() == Freeze.FreezeMode.Stationary))
            .build()
      );
   private final Setting<Double> driftSpeed = this.sgMace
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("drift-speed"))
                  .description("tick downNumber(0.05 = 1 /, fallDistance)"))
               .defaultValue(0.05)
               .range(0.01, 0.5)
               .sliderRange(0.01, 0.2)
               .visible(() -> this.mode.get() == Freeze.FreezeMode.Stationary && (Boolean)this.maceMode.get()))
            .build()
      );
   private final Setting<Double> maxDrift = this.sgMace
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("max-drift"))
                  .description("mostbigdownDistance(toafternotdown, Service S08 afterwillReset)"))
               .defaultValue(5.0)
               .range(1.0, 20.0)
               .sliderRange(1.0, 10.0)
               .visible(() -> this.mode.get() == Freeze.FreezeMode.Stationary && (Boolean)this.maceMode.get()))
            .build()
      );
   private int missedOutTick = 0;
   private boolean warpInProgress = false;
   private boolean isFlushing = false;
   private final List<Packet<?>> queuedPackets = new ArrayList<>();
   private double frozenX;
   private double frozenY;
   private double frozenZ;
   private float frozenYaw;
   private float frozenPitch;
   private double accumulatedDrift = 0.0;
   private float prevYawOffset = 0.0F;
   private float prevPitchOffset = 0.0F;
   private final Random random = new Random();

   public Freeze() {
      super(
         AddonTemplate.CATEGORY,
         "Freeze",
         "Freezes your position. Stationary mode tolerates Grim (no S08 disappear, holds move packets). Mace mode slows your fall to boost smash damage. Freezes via queueing or cancelling movement packets and can also freeze your look angle. Partly unusable."
      );
   }

   public void onActivate() {
      if (this.mc.player == null) {
         this.toggle();
      } else {
         this.missedOutTick = 0;
         this.queuedPackets.clear();
         this.warpInProgress = false;
         this.accumulatedDrift = 0.0;
         this.frozenX = this.mc.player.getX();
         this.frozenY = this.mc.player.getY();
         this.frozenZ = this.mc.player.getZ();
         this.frozenYaw = this.mc.player.getYaw();
         this.frozenPitch = this.mc.player.getPitch();
      }
   }

   public void onDeactivate() {
      if ((Boolean)this.balance.get() && this.mc.player != null) {
         for (this.warpInProgress = true; this.missedOutTick > 0; this.missedOutTick--) {
            this.mc.player.tick();
         }

         this.warpInProgress = false;
      }

      if (this.mode.get() == Freeze.FreezeMode.Queue && !this.queuedPackets.isEmpty() && this.mc.getNetworkHandler() != null) {
         this.isFlushing = true;

         for (Packet<?> packet : this.queuedPackets) {
            this.mc.getNetworkHandler().sendPacket(packet);
         }

         this.queuedPackets.clear();
         this.isFlushing = false;
      }

      this.missedOutTick = 0;
      if ((Boolean)this.bypassNegativeTimer.get()) {
         this.interact();
      }
   }

   @EventHandler
   public void onTickPre(Pre event) {
      if (!this.warpInProgress) {
         this.missedOutTick++;
         if (this.mc.player != null) {
            this.mc.player.setVelocity(Vec3d.ZERO);
            if (this.mode.get() == Freeze.FreezeMode.Stationary && (Boolean)this.maceMode.get() && this.accumulatedDrift < (Double)this.maxDrift.get()) {
               double nextY = this.frozenY - (Double)this.driftSpeed.get();
               if (this.mc.world != null && this.mc.player != null) {
                  Box nextBox = this.mc.player.getBoundingBox().offset(0.0, nextY - this.frozenY, 0.0);
                  if (this.mc.world.isSpaceEmpty(this.mc.player, nextBox)) {
                     this.frozenY = nextY;
                     this.accumulatedDrift = this.accumulatedDrift + (Double)this.driftSpeed.get();
                  } else {
                     this.accumulatedDrift = (Double)this.maxDrift.get();
                  }
               } else {
                  this.frozenY = this.frozenY - (Double)this.driftSpeed.get();
                  this.accumulatedDrift = this.accumulatedDrift + (Double)this.driftSpeed.get();
               }
            }

            this.mc.player.setPosition(this.frozenX, this.frozenY, this.frozenZ);
         }
      }
   }

   @EventHandler
   public void onPlayerMove(PlayerMoveEvent event) {
      event.movement = Vec3d.ZERO;
   }

   @EventHandler
   public void onPacketReceive(Receive event) {
      if (event.packet instanceof PlayerPositionLookS2CPacket) {
         this.missedOutTick = 0;
         if (this.mc.player != null) {
            this.frozenYaw = this.mc.player.getYaw();
            this.frozenPitch = this.mc.player.getPitch();
            this.frozenX = this.mc.player.getX();
            this.frozenY = this.mc.player.getY();
            this.frozenZ = this.mc.player.getZ();
         }

         this.accumulatedDrift = 0.0;
         if ((Boolean)this.disableOnFlag.get()) {
            if ((Boolean)this.notification.get()) {
               ChatUtil.sendFormatted("&c[Freeze] &7Flagged - disabled");
            }

            this.toggle();
         }
      }
   }

   @EventHandler
   public void onPacketSend(Send event) {
      if (!this.warpInProgress && !this.isFlushing) {
         Packet<?> packet = event.packet;
         switch ((Freeze.FreezeMode)this.mode.get()) {
            case Queue:
               this.handleQueue(event, packet);
               break;
            case Cancel:
               this.handleCancel(event, packet);
               break;
            case Stationary:
               this.handleStationary(event, packet);
         }
      }
   }

   private void handleQueue(Send event, Packet<?> packet) {
      this.queuedPackets.add(packet);
      event.cancel();
   }

   private void handleCancel(Send event, Packet<?> packet) {
      if (packet instanceof PlayerMoveC2SPacket) {
         event.cancel();
      }
   }

   private void handleStationary(Send event, Packet<?> packet) {
      if (packet instanceof PlayerMoveC2SPacket movePacket) {
         PlayerMoveC2SPacket replacement = this.replaceMovePacket(movePacket);
         if (replacement != movePacket) {
            event.cancel();
            this.isFlushing = true;
            this.sendPacketRaw(replacement);
            this.isFlushing = false;
         }
      } else if (packet instanceof CommonPongC2SPacket) {
         if ((Boolean)this.cancelC0B.get()) {
            event.cancel();
         }
      }
   }

   private PlayerMoveC2SPacket replaceMovePacket(PlayerMoveC2SPacket original) {
      boolean onGround = this.mc.player != null && this.mc.player.isOnGround();
      boolean horizCollision = this.mc.player != null && this.mc.player.horizontalCollision;
      if (original.changesPosition() && original.changesLook()) {
         return new PositionAndOnGround(this.frozenX, this.frozenY, this.frozenZ, onGround, horizCollision);
      } else if (original.changesPosition()) {
         return new PositionAndOnGround(this.frozenX, this.frozenY, this.frozenZ, onGround, horizCollision);
      } else {
         return (PlayerMoveC2SPacket)(original.changesLook() && this.freezeLook.get()
            ? new LookAndOnGround(this.frozenYaw, this.frozenPitch, onGround, horizCollision)
            : original);
      }
   }

   private void sendPacketRaw(Packet<?> packet) {
      if (this.mc.getNetworkHandler() != null) {
         this.mc.getNetworkHandler().sendPacket(packet);
      }
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      if (this.isActive()) {
         this.toggle();
      }
   }

   @EventHandler
   private void onEntityRemoved(EntityRemovedEvent event) {
      if (event.entity == this.mc.player && this.isActive()) {
         this.toggle();
      }
   }

   private float generateYawOffset() {
      float offset;
      do {
         offset = (float)(0.002 + this.random.nextDouble() * 0.008);
      } while (Math.abs(offset - this.prevYawOffset) < 1.0E-6F);

      this.prevYawOffset = offset;
      return offset;
   }

   private float generatePitchOffset() {
      float offset;
      do {
         offset = (float)(0.002 + this.random.nextDouble() * 0.008);
      } while (Math.abs(offset - this.prevPitchOffset) < 1.0E-6F);

      this.prevPitchOffset = offset;
      return offset;
   }

   private boolean isInteractable(ItemStack stack) {
      if (stack.getItem() != Items.ENDER_PEARL
         && stack.getItem() != Items.TNT
         && stack.getItem() != Items.FIRE_CHARGE
         && stack.getItem() != Items.WIND_CHARGE) {
         UseAction action = stack.getUseAction();
         return action != UseAction.EAT && action != UseAction.DRINK && action != UseAction.BOW && action != UseAction.CROSSBOW;
      } else {
         return false;
      }
   }

   private void interact() {
      if (this.mc.player != null && this.mc.interactionManager != null && this.mc.world != null) {
         InventoryAccessor inv = (InventoryAccessor)this.mc.player.getInventory();
         Hand hand = Hand.OFF_HAND;
         int prevSlot = -1;
         if (!this.isInteractable(this.mc.player.getStackInHand(Hand.OFF_HAND))) {
            for (int i = 0; i <= 8; i++) {
               if (this.isInteractable(this.mc.player.getInventory().getStack(i))) {
                  hand = Hand.MAIN_HAND;
                  if (i != inv.getSelectedSlot()) {
                     prevSlot = inv.getSelectedSlot();
                     inv.setSelectedSlot(i);
                  }
                  break;
               }
            }
         }

         this.mc.interactionManager.interactItem(this.mc.player, hand);
         if (prevSlot != -1) {
            inv.setSelectedSlot(prevSlot);
         }
      }
   }

   public static enum FreezeMode {
      Queue,
      Cancel,
      Stationary;
   }
}
