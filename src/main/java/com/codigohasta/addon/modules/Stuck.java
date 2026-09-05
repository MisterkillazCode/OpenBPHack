package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.lang.reflect.Field;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

public class Stuck extends Module {
   public static Stuck INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Stuck.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Mode")).description("NoPacket: DisappearhaveMovePack(AirinCanUse) | CancelMove: (GroundFaceSet)"))
               .defaultValue(Stuck.Mode.NoPacket))
            .build()
      );
   private static final int CYCLE_LENGTH = 20;
   private static final int UNFREEZE_START = 19;
   private int cycleTick;
   private float lastYaw;
   private float lastPitch;
   private boolean bypassPacket;
   private Field velocityEntityIdField;

   public Stuck() {
      super(AddonTemplate.CATEGORY, "Stuck", "Frees you when stuck inside a block by nudging or teleporting out.");
      INSTANCE = this;
   }

   public void onActivate() {
      if (this.mc.player != null) {
         this.lastYaw = this.mc.player.getYaw();
         this.lastPitch = this.mc.player.getPitch();
      }

      this.bypassPacket = false;
      this.cycleTick = 0;
      this.initReflection();
   }

   public void onDeactivate() {
      if (this.mode.get() == Stuck.Mode.NoPacket && this.mc.player != null && !this.mc.player.isOnGround()) {
         this.bypassPacket = true;
         this.mc
            .player
            .networkHandler
            .sendPacket(
               new Full(
                  this.mc.player.getX() + 1337.0,
                  this.mc.player.getY(),
                  this.mc.player.getZ() + 1337.0,
                  this.mc.player.getYaw() + 0.01F,
                  this.mc.player.getPitch(),
                  this.mc.player.isOnGround(),
                  this.mc.player.horizontalCollision
               )
            );
         this.bypassPacket = false;
      }
   }

   public String getInfoString() {
      return ((Stuck.Mode)this.mode.get()).title;
   }

   private void initReflection() {
      try {
         for (Field f : EntityVelocityUpdateS2CPacket.class.getDeclaredFields()) {
            f.setAccessible(true);
            if (f.getType() == int.class || f.getType() == int.class) {
               this.velocityEntityIdField = f;
               break;
            }
         }
      } catch (Exception var5) {
      }
   }

   private boolean shouldFreeze() {
      return this.cycleTick % 20 < 19;
   }

   private void zeroInput() {
      if (this.mc.player != null) {
         this.mc.player.input.playerInput = new PlayerInput(false, false, false, false, false, false, false);
      }
   }

   @EventHandler
   private void onTickPre(Pre event) {
      if (this.mc.player != null) {
         this.cycleTick++;
         this.zeroInput();
         if (this.mode.get() == Stuck.Mode.NoPacket) {
            float yaw = this.mc.player.getYaw();
            float pitch = this.mc.player.getPitch();
            if (yaw != this.lastYaw || pitch != this.lastPitch) {
               this.bypassPacket = true;
               this.mc.player.networkHandler.sendPacket(new LookAndOnGround(yaw, pitch, this.mc.player.isOnGround(), this.mc.player.horizontalCollision));
               this.bypassPacket = false;
            }

            this.lastYaw = yaw;
            this.lastPitch = pitch;
         }
      }
   }

   @EventHandler
   private void onPlayerMove(PlayerMoveEvent event) {
      if (this.mode.get() == Stuck.Mode.CancelMove) {
         if (this.shouldFreeze()) {
            ((IVec3d)event.movement).meteor$set(0.0, 0.0, 0.0);
            this.mc.player.setVelocity(Vec3d.ZERO);
         }
      }
   }

   @EventHandler
   private void onPacketSend(Send event) {
      if (!this.bypassPacket) {
         if (this.mode.get() == Stuck.Mode.NoPacket) {
            if (event.packet instanceof PlayerMoveC2SPacket) {
               event.cancel();
            }

            if (event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
               try {
                  if (this.velocityEntityIdField != null && this.velocityEntityIdField.getInt(packet) == this.mc.player.getId()) {
                     event.cancel();
                  }
               } catch (Exception var4) {
               }
            }
         }
      }
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      if (event.packet instanceof PlayerPositionLookS2CPacket) {
         this.toggle();
         this.sendToggledMsg();
      }
   }

   public static enum Mode {
      NoPacket("NoPacket"),
      CancelMove("CancelMove");

      private final String title;

      private Mode(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }
}
