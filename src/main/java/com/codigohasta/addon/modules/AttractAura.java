package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class AttractAura extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgBypass = this.settings.createGroup("pastSetting (Defense)");
   private final SettingGroup sgFilters = this.settings.createGroup("Targetpast");
   private final Setting<AttractAura.Mode> mode = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("Mode")).defaultValue(AttractAura.Mode.Crosshair)).build());
   private final Setting<AttractAura.ToggleBehavior> toggleBehavior = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("inKeyMoveDo")).defaultValue(AttractAura.ToggleBehavior.StopTracking)).build());
   private final Setting<Double> range = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("Range"))
            .defaultValue(30.0)
            .min(0.0)
            .sliderMax(100.0)
            .build()
      );
   private final Setting<Double> speed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("Speed"))
            .defaultValue(1.0)
            .min(0.0)
            .sliderMax(5.0)
            .build()
      );
   private final Setting<Double> stopDistance = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
               .name("StopStopDistance"))
            .defaultValue(1.5)
            .min(0.0)
            .sliderMax(10.0)
            .build()
      );
   private final Setting<Boolean> lookAt = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Auto"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> middleClickLock = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("inKeyLock"))
               .defaultValue(true))
            .build()
      );
   private final Setting<AttractAura.AntiKickMode> antiKick = this.sgBypass
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("AntiMode")).description("downMove: OneTimeForcedownOnedown; GroundBuild: tellServeryouatGroundup."))
               .defaultValue(AttractAura.AntiKickMode.DownJitter))
            .build()
      );
   private final Setting<Integer> kickDelay = this.sgBypass
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("CheckTestRate"))
                     .description("manyfewTickRowOneTimesAntiMoveDo."))
                  .defaultValue(25))
               .min(5)
               .visible(() -> this.antiKick.get() != AttractAura.AntiKickMode.None))
            .build()
      );
   private final Setting<Boolean> ignoreFriends = this.sgFilters
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("friend"))
               .defaultValue(true))
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
   private PlayerEntity target;
   private int antiKickTimer = 0;

   public AttractAura() {
      super(AddonTemplate.CATEGORY, "AttractAura", "Pulls nearby players toward you like a gravity aura.");
   }

   public void onActivate() {
      this.target = null;
      this.antiKickTimer = 0;
   }

   private boolean isTargetValid(PlayerEntity player) {
      if (player != null && player != this.mc.player && player.isAlive()) {
         if (this.mode.get() != AttractAura.Mode.Lock && this.mc.player.distanceTo(player) > (Double)this.range.get() + 30.0) {
            return false;
         } else if ((Boolean)this.ignoreFriends.get() && !Friends.get().shouldAttack(player)) {
            return false;
         } else if (this.mc.getNetworkHandler() == null) {
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
               } else {
                  return gm == GameMode.CREATIVE ? (Boolean)this.followCreative.get() : false;
               }
            }
         }
      } else {
         return false;
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

   @EventHandler
   private void onMouseClick(MouseClickEvent event) {
      if ((Boolean)this.middleClickLock.get() && event.button() == 2 && event.action == KeyAction.Press && this.mc.currentScreen == null) {
         if (this.mode.get() == AttractAura.Mode.Lock) {
            this.target = null;
            if (this.toggleBehavior.get() == AttractAura.ToggleBehavior.StopTracking) {
               this.mode.set(AttractAura.Mode.None);
               this.info("PowerAlreadyRemove (Mode: no)", new Object[0]);
            } else {
               this.mode.set(AttractAura.Mode.Crosshair);
               this.info("AlreadySwitchMode", new Object[0]);
            }

            return;
         }

         PlayerEntity lookedAt = this.getTargetByCrosshair();
         if (lookedAt != null) {
            this.target = lookedAt;
            this.mode.set(AttractAura.Mode.Lock);
            this.info("PowerLock Target:" + lookedAt.getName().getString(), new Object[0]);
         }
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.mode.get() == AttractAura.Mode.None) {
            this.target = null;
         } else {
            if (this.mode.get() == AttractAura.Mode.Lock) {
               if (this.target != null && !this.isTargetValid(this.target)) {
                  this.target = null;
                  if (this.toggleBehavior.get() == AttractAura.ToggleBehavior.StopTracking) {
                     this.mode.set(AttractAura.Mode.None);
                  } else {
                     this.mode.set(AttractAura.Mode.Crosshair);
                  }

                  this.info("Lock TargetDisappear, ResetStatus", new Object[0]);
               }
            } else {
               KillAura aura = (KillAura)Modules.get().get(KillAura.class);
               if (aura != null && aura.isActive() && aura.getTarget() instanceof PlayerEntity auraPlayer && this.isTargetValid(auraPlayer)) {
                  this.target = auraPlayer;
               } else if (this.mode.get() == AttractAura.Mode.Nearest) {
                  this.target = null;
                  double bestDist = Double.MAX_VALUE;

                  for (PlayerEntity p : this.mc.world.getPlayers()) {
                     if (this.isTargetValid(p) && this.mc.player.distanceTo(p) < bestDist) {
                        bestDist = this.mc.player.distanceTo(p);
                        this.target = p;
                     }
                  }
               } else if (this.mode.get() == AttractAura.Mode.Crosshair) {
                  this.target = this.getTargetByCrosshair();
               }
            }

            if (this.target != null) {
               Vec3d targetPos = this.target.getBoundingBox().getCenter();
               Vec3d playerPos = this.mc.player.getEyePos();
               double distance = playerPos.distanceTo(targetPos);
               if (distance > (Double)this.stopDistance.get()) {
                  Vec3d dir = targetPos.subtract(playerPos).normalize();
                  double velX = dir.x * (Double)this.speed.get();
                  double velY = dir.y * (Double)this.speed.get();
                  double velZ = dir.z * (Double)this.speed.get();
                  this.antiKickTimer++;
                  if (this.antiKickTimer >= (Integer)this.kickDelay.get()) {
                     if (this.antiKick.get() == AttractAura.AntiKickMode.DownJitter) {
                        velY = -0.05;
                     } else if (this.antiKick.get() == AttractAura.AntiKickMode.FloorFake && this.mc.getNetworkHandler() != null) {
                        this.mc.getNetworkHandler().sendPacket(new OnGroundOnly(true, this.mc.player.horizontalCollision));
                     }

                     this.antiKickTimer = 0;
                  }

                  this.mc.player.setVelocity(velX, velY, velZ);
                  if ((Boolean)this.lookAt.get()) {
                     double diffX = targetPos.x - playerPos.x;
                     double diffY = targetPos.y - playerPos.y;
                     double diffZ = targetPos.z - playerPos.z;
                     double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
                     this.mc.player.setYaw((float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
                     this.mc.player.setPitch((float)(-Math.toDegrees(Math.atan2(diffY, diffXZ))));
                  }
               } else {
                  this.mc.player.setVelocity(0.0, 0.0, 0.0);
               }
            }
         }
      }
   }

   public String getInfoString() {
      return this.target != null ? this.target.getName().getString() : ((AttractAura.Mode)this.mode.get()).toString();
   }

   public static enum AntiKickMode {
      None,
      DownJitter,
      FloorFake;
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
