package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class ElytraFollower extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgFlight = this.settings.createGroup("StartandView");
   private final SettingGroup sgFilters = this.settings.createGroup("Targetpast");
   private final Setting<ElytraFollower.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Mode")).description("SelectTarget'sDirection. SettingFor \"no\" TimeModuleStopStop."))
               .defaultValue(ElytraFollower.Mode.Crosshair))
            .build()
      );
   private final Setting<ElytraFollower.ToggleBehavior> toggleBehavior = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("inKeyMoveDo")).description("AlreadyLock TargetTime, TimesinKey'sRowFor."))
               .defaultValue(ElytraFollower.ToggleBehavior.StopTracking))
            .build()
      );
   private final Setting<Boolean> lockAnytime = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("RandomTimeLock"))
                  .description("Enableafter, MakenotalsocanpastinKeyLock Target."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> middleClickLock = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("inKeyLock"))
                  .description("AllowUseMarkinKeyLockPoint'sPlayer."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> range = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("Range"))
               .description("Player'smostbigDistance."))
            .defaultValue(150.0)
            .min(0.0)
            .sliderMax(500.0)
            .build()
      );
   private final Setting<Double> speed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("Fly Speed"))
               .description("Time'sEnterSpeed."))
            .defaultValue(1.8)
            .min(0.0)
            .sliderMax(5.0)
            .build()
      );
   private final Setting<Double> stopDistance = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("StopStopDistance"))
               .description("DistanceTargetmanyTimeStopStopEnter, DefenseStop."))
            .defaultValue(2.5)
            .min(0.0)
            .sliderMax(10.0)
            .build()
      );
   private final Setting<Boolean> lookAt = this.sgFlight
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Auto"))
                  .description("FlyTimeAutolookTarget."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> autoTakeoff = this.sgFlight
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AutoStart"))
                  .description("GroundTimeAutoJumpEnable."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> takeoffDelay = this.sgFlight
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("StartDelay"))
                  .description("AutoStartMoveDoofBetween'sCheckTestDelay."))
               .defaultValue(5))
            .min(0)
            .build()
      );
   private final Setting<Boolean> followSurvival = this.sgFilters
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("SpawnMode"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> followAdventure = this.sgFilters
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Mode"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> followCreative = this.sgFilters
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("BuildMode"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> followSpectator = this.sgFilters
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Mode"))
               .defaultValue(false))
            .build()
      );
   private PlayerEntity target;
   private int takeoffTimer = 0;

   public ElytraFollower() {
      super(AddonTemplate.CATEGORY, "ElytraFollower", "Follows another player while elytra flying, maintaining a set distance. (Unreliable.)");
   }

   public void onActivate() {
      this.target = null;
      this.takeoffTimer = 0;
   }

   private boolean isTargetValid(PlayerEntity player) {
      if (player == null || player == this.mc.player) {
         return false;
      } else if (!player.isAlive()) {
         return false;
      } else if (this.mc.player.distanceTo(player) > (Double)this.range.get() + 30.0) {
         return false;
      } else if (!Friends.get().shouldAttack(player)) {
         return false;
      } else {
         PlayerListEntry entry = this.mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
         if (entry == null) {
            return false;
         } else {
            GameMode gm = entry.getGameMode();
            if (gm == GameMode.SURVIVAL) {
               return (Boolean)this.followSurvival.get();
            } else if (gm == GameMode.ADVENTURE) {
               return (Boolean)this.followAdventure.get();
            } else if (gm == GameMode.CREATIVE) {
               return (Boolean)this.followCreative.get();
            } else {
               return gm == GameMode.SPECTATOR ? (Boolean)this.followSpectator.get() : false;
            }
         }
      }
   }

   private PlayerEntity getTargetByCrosshair() {
      PlayerEntity best = null;
      double bestDiff = Double.MAX_VALUE;

      for (PlayerEntity player : this.mc.world.getPlayers()) {
         if (this.isTargetValid(player)) {
            double diffX = player.getX() - this.mc.player.getX();
            double diffZ = player.getZ() - this.mc.player.getZ();
            float targetYaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
            float diff = Math.abs(MathHelper.wrapDegrees(this.mc.player.getYaw() - targetYaw));
            if (diff < bestDiff) {
               bestDiff = diff;
               best = player;
            }
         }
      }

      return best;
   }

   private PlayerEntity findBestTarget() {
      if (this.mode.get() == ElytraFollower.Mode.None) {
         return null;
      } else if (this.mode.get() == ElytraFollower.Mode.Nearest) {
         PlayerEntity best = null;
         double bestDist = Double.MAX_VALUE;

         for (PlayerEntity player : this.mc.world.getPlayers()) {
            if (this.isTargetValid(player)) {
               double dist = this.mc.player.distanceTo(player);
               if (dist < bestDist) {
                  bestDist = dist;
                  best = player;
               }
            }
         }

         return best;
      } else {
         return this.mode.get() == ElytraFollower.Mode.Crosshair ? this.getTargetByCrosshair() : null;
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.mode.get() == ElytraFollower.Mode.None) {
            this.target = null;
         } else {
            if (this.mode.get() == ElytraFollower.Mode.Lock) {
               if (this.target != null && !this.isTargetValid(this.target)) {
                  this.target = null;
                  if (this.toggleBehavior.get() == ElytraFollower.ToggleBehavior.StopTracking) {
                     this.mode.set(ElytraFollower.Mode.None);
                  } else {
                     this.mode.set(ElytraFollower.Mode.Crosshair);
                  }

                  this.info("Lock TargetEffect, AlreadyReset", new Object[0]);
               }
            } else {
               KillAura aura = (KillAura)Modules.get().get(KillAura.class);
               if (aura != null && aura.isActive() && aura.getTarget() instanceof PlayerEntity auraPlayer && this.isTargetValid(auraPlayer)) {
                  this.target = auraPlayer;
               } else {
                  this.target = this.findBestTarget();
               }
            }

            ItemStack chest = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
            if (chest.isOf(Items.ELYTRA)) {
               if ((Boolean)this.autoTakeoff.get() && this.mc.player.getPose() != EntityPose.GLIDING) {
                  if (this.takeoffTimer > 0) {
                     this.takeoffTimer--;
                  } else if (this.mc.player.isOnGround()) {
                     this.mc.player.jump();
                     this.takeoffTimer = (Integer)this.takeoffDelay.get();
                  } else if (!this.mc.player.isSubmergedInWater()) {
                     this.mc
                        .getNetworkHandler()
                        .sendPacket(
                           new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING)
                        );
                     this.takeoffTimer = (Integer)this.takeoffDelay.get();
                  }
               } else {
                  if (this.mc.player.getPose() == EntityPose.GLIDING && this.target != null) {
                     Vec3d targetPos = this.target.getBoundingBox().getCenter();
                     Vec3d playerPos = this.mc.player.getEyePos();
                     double distance = playerPos.distanceTo(targetPos);
                     if (distance > (Double)this.stopDistance.get()) {
                        Vec3d dir = targetPos.subtract(playerPos).normalize();
                        this.mc
                           .player
                           .setVelocity(
                              dir.x * (Double)this.speed.get(), dir.y * (Double)this.speed.get(), dir.z * (Double)this.speed.get()
                           );
                        if ((Boolean)this.lookAt.get()) {
                           double diffX = targetPos.x - playerPos.x;
                           double diffY = targetPos.y - playerPos.y;
                           double diffZ = targetPos.z - playerPos.z;
                           double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
                           this.mc.player.setYaw((float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
                           this.mc.player.setPitch((float)(-Math.toDegrees(Math.atan2(diffY, diffXZ))));
                        }
                     } else {
                        this.mc.player.setVelocity(this.mc.player.getVelocity().multiply(0.5));
                     }
                  }
               }
            }
         }
      }
   }

   public String getInfoString() {
      return this.target != null ? this.target.getName().getString() : ((ElytraFollower.Mode)this.mode.get()).toString();
   }

   public static enum Mode {
      Nearest,
      Crosshair,
      Lock,
      None;
   }

   public static enum ToggleBehavior {
      BackToCrosshair,
      StopTracking;
   }
}
