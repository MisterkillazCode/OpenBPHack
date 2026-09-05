package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.lang.reflect.Field;
import java.util.Random;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Sent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.Surround;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos.Mutable;

public class GrimCriticals extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<GrimCriticals.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("mode")).description("The mode to use for criticals.")).defaultValue(GrimCriticals.Mode.GrimV3))
            .build()
      );
   private final Setting<Boolean> multitask = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("multitask"))
                  .description("Allows criticals to work while using other modules like Surround."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> phasedOnly = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("phased-only"))
                     .description("Only performs criticals when phased (for Grim modes)."))
                  .defaultValue(true))
               .visible(() -> this.mode.get() == GrimCriticals.Mode.Grim || this.mode.get() == GrimCriticals.Mode.GrimV3))
            .build()
      );
   private final Setting<Boolean> wallsOnly = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("walls-only"))
                     .description("Only checks for walls when Phased Only is enabled."))
                  .defaultValue(true))
               .visible(() -> (this.mode.get() == GrimCriticals.Mode.Grim || this.mode.get() == GrimCriticals.Mode.GrimV3) && (Boolean)this.phasedOnly.get()))
            .build()
      );
   private final Setting<Boolean> moveFix = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("move-fix"))
                     .description("Prevents criticals while moving to avoid flag."))
                  .defaultValue(true))
               .visible(() -> this.mode.get() != GrimCriticals.Mode.Grim && this.mode.get() != GrimCriticals.Mode.GrimV3))
            .build()
      );
   private final Setting<Boolean> pauseOnCA = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("pause-on-ca"))
                  .description("Pauses Criticals when Crystal Aura is active."))
               .defaultValue(true))
            .build()
      );
   private final Random random = new Random();
   private long lastAttackTime = 0L;
   private boolean postUpdateGround = false;
   private boolean postUpdateSprint = false;

   public GrimCriticals() {
      super(AddonTemplate.CATEGORY, "GrimCriticals", "Land critical hits by adjusting your fall state. (Unreliable, prefer another module.)");
   }

   public String getInfoString() {
      return ((GrimCriticals.Mode)this.mode.get()).name();
   }

   @EventHandler
   private void onAttackEntity(AttackEntityEvent event) {
      if (event.entity != null) {
         if ((Boolean)this.pauseOnCA.get()) {
            CrystalAura ca = (CrystalAura)Modules.get().get(CrystalAura.class);
            if (ca != null && ca.isActive()) {
               return;
            }
         }

         if ((Boolean)this.multitask.get() || !Modules.get().isActive(Surround.class)) {
            Entity target = event.entity;
            if (target instanceof LivingEntity || target instanceof EndCrystalEntity) {
               if (this.mc.player == null) {
                  return;
               }

               if (!this.mc.player.isClimbing()
                  && !this.mc.player.isTouchingWater()
                  && !this.mc.player.hasVehicle()
                  && !this.mc.player.isUsingItem()) {
                  this.postUpdateSprint = this.mc.player.isSprinting();
                  if (this.postUpdateSprint) {
                     this.mc
                        .player
                        .networkHandler
                        .sendPacket(
                           new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.STOP_SPRINTING)
                        );
                  }

                  this.doCritical(target);
               }
            }
         }
      }
   }

   @EventHandler
   private void onPacketSent(Sent event) {
      if (this.mc.player != null) {
         if (event.packet instanceof PlayerMoveC2SPacket) {
            if (this.postUpdateGround) {
               this.setPacketOnGround((PlayerMoveC2SPacket)event.packet, true);
               this.postUpdateGround = false;
            }

            if (this.postUpdateSprint) {
               this.mc
                  .player
                  .networkHandler
                  .sendPacket(new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_SPRINTING));
               this.postUpdateSprint = false;
            }
         }
      }
   }

   private void doCritical(Entity target) {
      if (this.mc.player != null) {
         if (this.mc.player.isOnGround()) {
            if (!this.mc.options.jumpKey.isPressed()) {
               double x = this.mc.player.getX();
               double y = this.mc.player.getY();
               double z = this.mc.player.getZ();
               float yaw = this.mc.player.getYaw();
               float pitch = this.mc.player.getPitch();
               switch ((GrimCriticals.Mode)this.mode.get()) {
                  case Packet:
                     this.sendPosition(x, y + 0.0625, z, true);
                     this.sendPosition(x, y, z, false);
                     this.mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, this.mc.player.isSneaking()));
                     break;
                  case Bypass:
                     this.sendPosition(x, y + 0.11, z, false);
                     this.sendPosition(x, y + 0.1100013579, z, false);
                     this.sendPosition(x, y + 1.3579E-6, z, false);
                     this.mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, this.mc.player.isSneaking()));
                     break;
                  case Grim:
                     double offset1 = 0.1016 + this.random.nextDouble() * 0.001;
                     double offset2 = 0.0202 + this.random.nextDouble() * 0.001;
                     double offset3 = 3.239E-4 + this.random.nextDouble() * 1.0E-4;
                     this.sendPosition(x, y + offset1, z, false);
                     this.sendPosition(x, y + offset2, z, false);
                     this.sendPosition(x, y + offset3, z, false);
                     this.mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, this.mc.player.isSneaking()));
                     break;
                  case Strict:
                     if (System.currentTimeMillis() - this.lastAttackTime < 500L) {
                        return;
                     }

                     this.sendPosition(x, y + 0.0625, z, false);
                     this.sendPosition(x, y, z, false);
                     this.mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, this.mc.player.isSneaking()));
                     this.postUpdateGround = true;
                     this.lastAttackTime = System.currentTimeMillis();
                     break;
                  case GrimV3:
                     if (!(Boolean)this.phasedOnly.get() || (this.wallsOnly.get() ? this.isDoublePhased() : this.isPhased())) {
                        if ((Boolean)this.moveFix.get() && PlayerUtils.isMoving()) {
                           return;
                        }

                        if (System.currentTimeMillis() - this.lastAttackTime >= 250L || this.mc.player.fallDistance > 0.0) {
                           this.sendPositionFull(x, y + 0.0625, z, yaw, pitch, false);
                           this.sendPositionFull(x, y + 0.0625013579, z, yaw, pitch, false);
                           this.sendPositionFull(x, y + 1.3579E-6, z, yaw, pitch, false);
                           this.lastAttackTime = System.currentTimeMillis();
                        }
                     }
               }

               this.mc.player.swingHand(this.mc.player.getActiveHand());
            }
         }
      }
   }

   private void setPacketOnGround(PlayerMoveC2SPacket packet, boolean onGround) {
      try {
         Field field = PlayerMoveC2SPacket.class.getDeclaredField("onGround");
         field.setAccessible(true);
         field.setBoolean(packet, onGround);
      } catch (Exception var4) {
         var4.printStackTrace();
      }
   }

   private void sendPosition(double x, double y, double z, boolean onGround) {
      this.mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, onGround, false));
   }

   private void sendPositionFull(double x, double y, double z, float yaw, float pitch, boolean onGround) {
      this.mc.player.networkHandler.sendPacket(new Full(x, y, z, yaw, pitch, onGround, false));
   }

   private boolean isPhased() {
      if (this.mc.world != null && this.mc.player != null) {
         Box box = this.mc.player.getBoundingBox();
         Mutable mutable = new Mutable();

         for (int x = (int)Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int)Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
               for (int z = (int)Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                  mutable.set(x, y, z);
                  if (this.mc.world.getBlockState(mutable).isFullCube(this.mc.world, mutable)) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean isDoublePhased() {
      if (this.mc.world != null && this.mc.player != null) {
         Box box = this.mc.player.getBoundingBox();
         Mutable mutable = new Mutable();

         for (int x = (int)Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int)Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
               for (int z = (int)Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                  mutable.set(x, y, z);
                  boolean current = this.mc.world.getBlockState(mutable).isFullCube(this.mc.world, mutable);
                  mutable.set(x, y + 1, z);
                  boolean up = this.mc.world.getBlockState(mutable).isFullCube(this.mc.world, mutable);
                  if (current && up) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static enum Mode {
      Packet,
      Bypass,
      Grim,
      Strict,
      GrimV3;
   }
}
