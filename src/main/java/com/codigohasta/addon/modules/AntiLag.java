package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
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
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

public class AntiLag extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgTp = this.settings.createGroup("Tp Options");
   private final Setting<AntiLag.VersionType> version = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("server-version-mode")).description("1.16 ModeEnable TpUtil PathMethod."))
               .defaultValue(AntiLag.VersionType.MC1_16))
            .build()
      );
   private final Setting<Double> range = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("range"))
               .description("Trigger'smostbigDistance."))
            .defaultValue(100.0)
            .range(0.1, 2000.0)
            .build()
      );
   private final Setting<Integer> limitPerSecond = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("limit-per-second"))
                  .description("Allow'smostbigPositionPackAmount(DefenseStopby)."))
               .defaultValue(100))
            .range(1, 10000)
            .build()
      );
   private final Setting<Double> moveDistance = this.sgTp
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("move-distance"))
               .description("PullDistanceForlongDegree'slittleEnterPack"))
            .defaultValue(0.5)
            .range(0.01, 1.0)
            .build()
      );
   private final Setting<AntiLag.VClipMode> searchVclipMode = this.sgTp
      .add(((Builder)((Builder)new Builder().name("search-vclip-mode")).defaultValue(AntiLag.VClipMode.OnlyUp)).build());
   private final Setting<Double> searchFindStep = this.sgTp
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
               .name("search-find-step"))
            .defaultValue(1.8)
            .range(0.1, 5.0)
            .build()
      );
   private final Setting<Boolean> back = this.sgTp
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("back"))
                  .description("DisablejustAntiPull ."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> allowIntoVoid = this.sgTp
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("allow-into-void"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> printWhenTooManyPacket = this.sgTp
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("print-too-many-packets"))
               .defaultValue(true))
            .build()
      );
   private int lagCounter = 0;
   private long lastResetTime = System.currentTimeMillis();
   private boolean isRateLimited = false;

   public AntiLag() {
      super(AddonTemplate.CATEGORY, "AntiLag", "Defensive pull module (Gcore). Effect not fully verified.");
   }

   public void onActivate() {
      this.lagCounter = 0;
      this.lastResetTime = System.currentTimeMillis();
      this.isRateLimited = false;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (System.currentTimeMillis() - this.lastResetTime >= 1000L) {
            this.lagCounter = 0;
            this.lastResetTime = System.currentTimeMillis();
            this.isRateLimited = false;
         }

         boolean isMoving = this.mc.player.input.playerInput.forward()
            || this.mc.player.input.playerInput.backward()
            || this.mc.player.input.playerInput.left()
            || this.mc.player.input.playerInput.right();
         if (isMoving && this.mc.player.horizontalCollision && !(Boolean)this.back.get() && this.searchVclipMode.get() == AntiLag.VClipMode.OnlyUp) {
            this.mc
               .player
               .setPosition(
                  this.mc.player.getX(), this.mc.player.getY() + (Double)this.searchFindStep.get(), this.mc.player.getZ()
               );
         }
      }
   }

   @EventHandler
   private void onPacketSend(Send event) {
      if (this.mc.player != null) {
         if (event.packet instanceof PlayerMoveC2SPacket) {
            boolean isFlying = this.mc.player.isGliding()
               && this.mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().toString().contains("elytra");
            if (isFlying) {
               return;
            }

            this.lagCounter++;
            if (this.lagCounter > (Integer)this.limitPerSecond.get()) {
               event.cancel();
               this.isRateLimited = true;
               if ((Boolean)this.printWhenTooManyPacket.get() && this.lagCounter == (Integer)this.limitPerSecond.get() + 1) {
                  this.warning("§7LimitSystemtpDataPack.", new Object[0]);
                  this.mc
                     .particleManager
                     .addParticle(
                        ParticleTypes.CRIT,
                        this.mc.player.getX(),
                        this.mc.player.getY() + 1.0,
                        this.mc.player.getZ(),
                        0.0,
                        0.0,
                        0.0
                     );
               }
            }
         }
      }
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (event.packet instanceof PlayerPositionLookS2CPacket packet) {
            if (this.lagCounter > (Integer)this.limitPerSecond.get()) {
               return;
            }

            Vec3d serverPos = packet.change().position();
            Vec3d playerPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
            double dist = playerPos.distanceTo(serverPos);
            if (dist > (Double)this.range.get()) {
               return;
            }

            event.cancel();
            this.mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(packet.teleportId()));
            if (!(Boolean)this.back.get()) {
               if (!(Boolean)this.allowIntoVoid.get() && serverPos.y < this.mc.world.getBottomY()) {
                  return;
               }

               if (this.version.get() == AntiLag.VersionType.MC1_16) {
                  int steps = (int)Math.ceil(dist / (Double)this.moveDistance.get());

                  for (int i = 1; i <= steps; i++) {
                     double ratio = (double)i / steps;
                     double nextX = serverPos.x + (playerPos.x - serverPos.x) * ratio;
                     double nextY = serverPos.y + (playerPos.y - serverPos.y) * ratio;
                     double nextZ = serverPos.z + (playerPos.z - serverPos.z) * ratio;
                     this.sendFullMovePacket(nextX, nextY, nextZ, this.mc.player.isOnGround());
                  }
               } else {
                  this.sendFullMovePacket(playerPos.x, playerPos.y, playerPos.z, this.mc.player.isOnGround());
               }

               this.mc.player.setPosition(playerPos.x, playerPos.y, playerPos.z);
               this.lagCounter++;
            }
         }
      }
   }

   @EventHandler
   private void onPlayerMove(PlayerMoveEvent event) {
      if (this.isRateLimited) {
         ((IVec3d)event.movement).meteor$set(0.0, 0.0, 0.0);
      }
   }

   private void sendFullMovePacket(double x, double y, double z, boolean onGround) {
      if (this.mc.getNetworkHandler() != null) {
         this.mc.getNetworkHandler().sendPacket(new Full(x, y, z, this.mc.player.getYaw(), this.mc.player.getPitch(), onGround, false));
      }
   }

   public static enum VClipMode {
      OnlyUp,
      Down,
      Both;
   }

   public static enum VersionType {
      MC1_16("1.16 (TpUtil Path)"),
      MC1_9("1.9 (Direct)");

      private final String name;

      private VersionType(String name) {
         this.name = name;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }
}
