package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class SpearKill extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgBlink = this.settings.createGroup("BlinkSelect");
   private final SettingGroup sgLunge = this.settings.createGroup("LungeSelect");
   private final SettingGroup sgBlinkLunge = this.settings.createGroup("BlinkLungeSelect");
   private final Setting<Boolean> nonofall = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("ChargeTimeDisablenofall")).description("atfastdownLungeTimeDefenseStopFallHurt."))
               .defaultValue(true))
            .build()
      );
   private final Setting<SpearKill.Mode> mode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Mode"))
                  .description("Lunge=SpeedRise, Blink=DataPackDelay."))
               .defaultValue(SpearKill.Mode.Lunge))
            .build()
      );
   public final Setting<Double> maxrange = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("mostbigAimDistance"))
               .description("EntityCanbyAim'sDistance(Block)."))
            .defaultValue(256.0)
            .min(0.0)
            .sliderRange(0.0, 512.0)
            .build()
      );
   private final Setting<SpearKill.TargetListMode> targetListMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("TargetListMode"))
                  .description("WhiteNameSingle = OnlyAimthisEntity, BlackNameSingle = notAimthisEntity"))
               .defaultValue(SpearKill.TargetListMode.Blacklist))
            .build()
      );
   private final Setting<Set<EntityType<?>>> targetEntities = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)new meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder()
                  .name("Target Entity"))
               .description("wantWhiteNameSingle/BlackNameSingle'sEntity"))
            .onlyAttackable()
            .build()
      );
   private final Setting<Boolean> ignorefriends = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("friend")).description("nottofriendSendMoveStrike.")).defaultValue(true)).build());
   private final Setting<Boolean> blinkLunge = this.sgBlinkLunge
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("Blink+Lunge")).description("BlinkModeandLunge.")).defaultValue(false))
               .visible(() -> this.mode.get() == SpearKill.Mode.Blink))
            .build()
      );
   private final Setting<Double> blinkLungeStrength = this.sgBlinkLunge
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("LungestrongDegree"))
                  .description("UseTarget'sSpeed"))
               .defaultValue(1.0)
               .min(0.1)
               .sliderRange(0.1, 2.0)
               .visible(() -> this.mode.get() == SpearKill.Mode.Blink && (Boolean)this.blinkLunge.get()))
            .build()
      );
   private final Setting<Integer> blinkLungeTicks = this.sgBlinkLunge
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("LungeDelay"))
                     .description("LungebeforeCharge'sTickDegreeNumber"))
                  .defaultValue(15))
               .min(1)
               .sliderRange(1, 30)
               .visible(() -> this.mode.get() == SpearKill.Mode.Blink && (Boolean)this.blinkLunge.get()))
            .build()
      );
   private final Setting<Double> flushRange = this.sgBlink
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("NewDistance"))
                  .description("TriggerNewTimetoTarget'sDistance(Block)"))
               .defaultValue(3.0)
               .min(1.0)
               .sliderRange(1.0, 10.0)
               .visible(() -> this.mode.get() == SpearKill.Mode.Blink))
            .build()
      );
   private final Setting<Double> maxflushRange = this.sgBlink
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("ForceNewDistance"))
                  .description("ForceNew'sDistance"))
               .defaultValue(9.5)
               .min(1.0)
               .sliderRange(1.0, 20.0)
               .visible(() -> this.mode.get() == SpearKill.Mode.Blink && !(Boolean)this.blinkLunge.get()))
            .build()
      );
   private final Setting<Boolean> blinkAimbot = this.sgBlink
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("AutoAim")).description("Lock Target")).defaultValue(true))
               .visible(() -> this.mode.get() == SpearKill.Mode.Blink))
            .build()
      );
   private final Setting<Double> blinkDistanceBoost = this.sgBlink
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("DistanceRise"))
                  .description("AddtoStartPosition'soutsideBlock(longMoveDistance)"))
               .defaultValue(0.0)
               .min(0.0)
               .sliderRange(0.0, 10.0)
               .visible(() -> this.mode.get() == SpearKill.Mode.Blink))
            .build()
      );
   private final Setting<SpearKill.lungeMode> LungeDirectionMode = this.sgLunge
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("LungeDirection"))
                  .description(
                     "DirectionBased = FacingViewDirection, FromAbove = FromAboveStrike, AutoFromAbove = FromAboveStrike, RemoveupDirectionPositionFromAbovePositiontoTarget'sPathnoEffect."
                  ))
               .defaultValue(SpearKill.lungeMode.DirectionBased))
            .build()
      );
   private final Setting<Double> aboveHeight = this.sgLunge
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("upDirectionHeight"))
                  .description("TargetCenterupDirection'sBlockNumber."))
               .defaultValue(10.0)
               .min(5.0)
               .sliderRange(5.0, 50.0)
               .visible(
                  () -> this.mode.get() == SpearKill.Mode.Lunge
                     && (
                        this.LungeDirectionMode.get() == SpearKill.lungeMode.FromAbove
                           || this.LungeDirectionMode.get() == SpearKill.lungeMode.Auto_FromAboveFirst
                     )
               ))
            .build()
      );
   private final Setting<Double> aboveHeightdistance = this.sgLunge
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("upDirectionHeightTriggerDistance"))
                  .description("ifatDistanceinsidetoupDirectionTargetPosition, startTargetLunge."))
               .defaultValue(3.0)
               .min(1.0)
               .sliderRange(1.0, 10.0)
               .visible(
                  () -> this.mode.get() == SpearKill.Mode.Lunge
                     && (
                        this.LungeDirectionMode.get() == SpearKill.lungeMode.FromAbove
                           || this.LungeDirectionMode.get() == SpearKill.lungeMode.Auto_FromAboveFirst
                     )
               ))
            .build()
      );
   private final Setting<Boolean> checkdistanceinvalid = this.sgLunge
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("TriggerDistance"))
                     .description("CheckupDirectionPosition'swhetherhaveEffect, UseupDirectionHeightTriggerDistanceAsRadius."))
                  .defaultValue(true))
               .visible(() -> this.mode.get() == SpearKill.Mode.Lunge && this.LungeDirectionMode.get() == SpearKill.lungeMode.Auto_FromAboveFirst))
            .build()
      );
   public final Setting<Double> getLungeStrength = this.sgLunge
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("longSpearSpeed"))
                  .description("UsePlayer'sSpeedAmount, FacingTargetDirection."))
               .defaultValue(5.0)
               .min(0.0)
               .sliderRange(1.0, 10.0)
               .visible(() -> this.mode.get() == SpearKill.Mode.Lunge))
            .build()
      );
   private final Setting<Boolean> stop = this.sgLunge
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("toTargetTimeStopStop")).description("toTargetTimeStopStopLunge.")).defaultValue(true))
               .visible(() -> this.mode.get() == SpearKill.Mode.Lunge))
            .build()
      );
   public final Setting<Double> stopDistance = this.sgLunge
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("StopStopDistance"))
                  .description("StopStopTimeandEntityofBetween'sDistance."))
               .defaultValue(2.0)
               .min(0.0)
               .sliderRange(0.0, 10.0)
               .visible(() -> (Boolean)this.stop.get() && this.mode.get() == SpearKill.Mode.Lunge))
            .build()
      );
   private final Setting<Integer> getreadyticksModifier = this.sgLunge
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("LungeDelayDevice"))
                     .description("atLungebeforeWaitlongSpearjust'sTimethan."))
                  .defaultValue(100))
               .min(0)
               .sliderRange(0, 100)
               .visible(() -> this.mode.get() == SpearKill.Mode.Lunge))
            .build()
      );
   private final List<PlayerMoveC2SPacket> packets = new ArrayList<>();
   private boolean isBlinking = false;
   private boolean isFlushing = false;
   private Vec3d startPos = null;
   private boolean wasCharging = false;
   private double lastTargetDistance = Double.MAX_VALUE;
   private boolean wasApproaching = false;
   private Entity killtarget;
   private int blinkChargeTicks = 0;
   private int flushCooldown = 0;
   private boolean firstPhase = false;
   private Vec3d aboveTargetPos = null;
   private boolean wasNoFallEnabled = false;
   private boolean noFallToggled = false;
   private boolean currentlyCharging = false;
   private final Mutable mutablePos = new Mutable();
   private final Map<Vec3d, Boolean> positionCache = new HashMap<>();

   public SpearKill() {
      super(AddonTemplate.CATEGORY, "SpearKill", "Long-range spear damage with lunge and blink modes. Self-hosted.");
   }

   public void onActivate() {
      this.resetState();
   }

   public void onDeactivate() {
      this.flushPackets();
      this.resetState();
   }

   private void resetState() {
      synchronized (this.packets) {
         this.packets.clear();
      }

      this.isBlinking = false;
      this.isFlushing = false;
      this.startPos = null;
      this.wasCharging = false;
      this.lastTargetDistance = Double.MAX_VALUE;
      this.wasApproaching = false;
      this.killtarget = null;
      this.blinkChargeTicks = 0;
      this.flushCooldown = 0;
      this.firstPhase = false;
      this.aboveTargetPos = null;
      if (this.noFallToggled && this.wasNoFallEnabled) {
         ((NoFall)Modules.get().get(NoFall.class)).toggle();
      }

      this.noFallToggled = false;
      this.wasNoFallEnabled = false;
   }

   private boolean isUsingSpear() {
      if (this.mc.player == null) {
         return false;
      } else {
         String itemName = this.mc.player.getActiveItem().getItem().toString().toLowerCase();
         return itemName.contains("spear");
      }
   }

   @EventHandler
   private void onPreTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.currentlyCharging = this.isUsingSpear();
         if ((Boolean)this.nonofall.get()) {
            if (this.currentlyCharging && !this.noFallToggled) {
               this.wasNoFallEnabled = ((NoFall)Modules.get().get(NoFall.class)).isActive();
               if (this.wasNoFallEnabled) {
                  ((NoFall)Modules.get().get(NoFall.class)).toggle();
                  this.noFallToggled = true;
               }
            } else if (!this.currentlyCharging && this.noFallToggled) {
               if (this.wasNoFallEnabled) {
                  ((NoFall)Modules.get().get(NoFall.class)).toggle();
               }

               this.noFallToggled = false;
            }
         }

         if (this.mode.get() != SpearKill.Mode.Lunge) {
            if (this.currentlyCharging) {
               this.blinkChargeTicks++;
               if (this.killtarget == null || !this.killtarget.isAlive() || !this.canSeeTarget(this.killtarget)) {
                  this.killtarget = this.target();
                  this.lastTargetDistance = this.killtarget != null ? this.mc.player.distanceTo(this.killtarget) : Double.MAX_VALUE;
                  this.wasApproaching = false;
               }
            } else {
               this.blinkChargeTicks = 0;
               this.killtarget = null;
               this.lastTargetDistance = Double.MAX_VALUE;
               this.wasApproaching = false;
            }

            if (this.flushCooldown > 0) {
               this.flushCooldown--;
            }

            if (this.currentlyCharging && !this.wasCharging) {
               this.startBlink();
            }

            if (!this.currentlyCharging && this.wasCharging && this.isBlinking) {
               if (this.killtarget != null) {
                  this.rotateToTarget(this.killtarget);
               }

               this.flushPackets();
               this.isBlinking = false;
               this.startPos = null;
            }

            this.wasCharging = this.currentlyCharging;
            if (this.isBlinking && this.killtarget != null && this.currentlyCharging) {
               double currentDistance = this.mc.player.distanceTo(this.killtarget);
               boolean isApproaching = currentDistance < this.lastTargetDistance;
               boolean shouldFlush = false;
               if (currentDistance <= (Double)this.flushRange.get()) {
                  shouldFlush = true;
               } else if (this.wasApproaching && !isApproaching && currentDistance < 8.0) {
                  shouldFlush = true;
               } else if (!(Boolean)this.blinkLunge.get()
                  && this.startPos != null
                  && new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ())
                        .distanceTo(this.startPos)
                     >= (Double)this.maxflushRange.get()) {
                  this.flushPackets();
                  this.startBlink();
               }

               if (shouldFlush) {
                  this.rotateToTarget(this.killtarget);
                  this.flushPackets();
                  if ((Boolean)this.blinkLunge.get()) {
                     this.flushCooldown = (Integer)this.blinkLungeTicks.get();
                  }

                  this.isBlinking = true;
                  this.startPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
                  synchronized (this.packets) {
                     this.packets.clear();
                  }

                  this.lastTargetDistance = this.mc.player.distanceTo(this.killtarget);
                  this.wasApproaching = false;
               } else {
                  this.lastTargetDistance = currentDistance;
                  this.wasApproaching = isApproaching;
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.mode.get() != SpearKill.Mode.Lunge) {
            if (this.currentlyCharging && (Boolean)this.blinkAimbot.get() && this.killtarget != null) {
               this.rotateToTarget(this.killtarget);
            }

            if (this.currentlyCharging
               && (Boolean)this.blinkLunge.get()
               && this.killtarget != null
               && this.flushCooldown == 0
               && this.blinkChargeTicks >= (Integer)this.blinkLungeTicks.get()) {
               this.rotateToTarget(this.killtarget);
               Vec3d viewDir = Vec3d.fromPolar(this.mc.player.getPitch(), this.mc.player.getYaw());
               this.mc.player.setSprinting(true);
               this.mc.player.setVelocity(viewDir.multiply((Double)this.blinkLungeStrength.get()));
            }
         } else {
            if (this.currentlyCharging) {
               if (this.killtarget == null) {
                  this.killtarget = this.target();
               }

               if (this.killtarget != null && !this.killtarget.isAlive()) {
                  if ((Boolean)this.stop.get()) {
                     this.mc.player.setVelocity(0.0, 0.0, 0.0);
                     this.mc.player.setSprinting(false);
                  }

                  this.killtarget = null;
                  this.firstPhase = false;
                  this.aboveTargetPos = null;
               }

               if (this.killtarget == null || !(this.killtarget instanceof LivingEntity)) {
                  return;
               }

               if (!this.isValidTarget(this.killtarget)) {
                  return;
               }

               this.LUNGE();
            } else {
               this.killtarget = null;
               this.firstPhase = false;
               this.aboveTargetPos = null;
            }
         }
      }
   }

   private void LUNGE() {
      int readyTicks = this.mc.player.getActiveHand() == Hand.MAIN_HAND
         ? this.getReadyTicks(this.mc.player.getMainHandStack().getItem())
         : this.getReadyTicks(this.mc.player.getOffHandStack().getItem());
      this.rotateToTarget(this.killtarget);
      if (this.mc.player.getItemUseTime() > readyTicks) {
         Box playerBox = this.mc.player.getBoundingBox().expand((Double)this.stopDistance.get());
         Box targetBox = this.killtarget.getBoundingBox();
         boolean atTarget = playerBox.intersects(targetBox);
         if (atTarget) {
            if ((Boolean)this.stop.get()) {
               this.killtarget = null;
               this.mc.player.setVelocity(0.0, 0.0, 0.0);
               this.mc.player.setSprinting(false);
            }

            this.firstPhase = false;
            this.aboveTargetPos = null;
            return;
         }

         double lungeSpeed = (Double)this.getLungeStrength.get();
         if (this.killtarget == null) {
            return;
         }

         switch ((SpearKill.lungeMode)this.LungeDirectionMode.get()) {
            case DirectionBased:
               Vec3d targetDir = this.killtarget.getBoundingBox().getCenter().subtract(this.mc.player.getEntityPos()).normalize();
               this.mc.player.setSprinting(true);
               this.mc.player.setVelocity(targetDir.multiply(lungeSpeed));
               break;
            case FromAbove:
               if (!this.firstPhase || this.aboveTargetPos == null) {
                  Vec3d targetCenter = this.killtarget.getBoundingBox().getCenter();
                  this.aboveTargetPos = new Vec3d(targetCenter.x, targetCenter.y + (Double)this.aboveHeight.get(), targetCenter.z);
                  this.firstPhase = true;
               }

               Vec3d playerPos = this.mc.player.getEntityPos();
               double distToAbove = playerPos.distanceTo(this.aboveTargetPos);
               Vec3d viewDir;
               if (distToAbove < (Double)this.aboveHeightdistance.get()) {
                  Vec3d targetDirx = this.killtarget.getBoundingBox().getCenter().subtract(playerPos).normalize();
                  viewDir = targetDirx;
                  this.firstPhase = false;
               } else {
                  Vec3d aboveDir = this.aboveTargetPos.subtract(playerPos).normalize();
                  viewDir = aboveDir;
               }

               this.mc.player.setSprinting(true);
               this.mc.player.setVelocity(viewDir.multiply(lungeSpeed));
               break;
            case Auto_FromAboveFirst:
               if (!this.firstPhase || this.aboveTargetPos == null) {
                  Vec3d targetCenter = this.killtarget.getBoundingBox().getCenter();
                  this.aboveTargetPos = new Vec3d(targetCenter.x, targetCenter.y + (Double)this.aboveHeight.get(), targetCenter.z);
                  this.firstPhase = true;
               }

               Vec3d playerPos = this.mc.player.getEntityPos();
               double distToAbove = playerPos.distanceTo(this.aboveTargetPos);
               boolean FromAbovePathValid = this.isFromAbovePathValid(this.aboveTargetPos, this.killtarget);
               Vec3d viewDir;
               if (!FromAbovePathValid) {
                  Vec3d targetDirx = this.killtarget.getBoundingBox().getCenter().subtract(playerPos).normalize();
                  viewDir = targetDirx;
                  this.firstPhase = false;
               } else if (distToAbove < (Double)this.aboveHeightdistance.get()) {
                  Vec3d targetDirx = this.killtarget.getBoundingBox().getCenter().subtract(playerPos).normalize();
                  viewDir = targetDirx;
                  this.firstPhase = false;
               } else {
                  Vec3d aboveDir = this.aboveTargetPos.subtract(playerPos).normalize();
                  viewDir = aboveDir;
               }

               this.mc.player.setSprinting(true);
               this.mc.player.setVelocity(viewDir.multiply(lungeSpeed));
         }
      }
   }

   private boolean isFromAbovePathValid(Vec3d abovePos, Entity target) {
      if (this.mc.world != null && abovePos != null) {
         Vec3d targetCenter = target.getBoundingBox().getCenter();
         if (this.invalid(abovePos)) {
            return false;
         } else {
            if ((Boolean)this.checkdistanceinvalid.get()) {
               double checkDistance = (Double)this.aboveHeightdistance.get();
               int radius = (int)checkDistance;

               for (int x = -radius; x <= radius; x++) {
                  for (int y = -radius; y <= radius; y++) {
                     for (int z = -radius; z <= radius; z++) {
                        Vec3d testPos = abovePos.add(x, y, z);
                        if (testPos.distanceTo(abovePos) <= checkDistance && this.invalid(testPos)) {
                           return false;
                        }
                     }
                  }
               }
            }

            int pathSteps = Math.max(10, (int)(abovePos.distanceTo(targetCenter) * 2.5));

            for (int i = 1; i < pathSteps; i++) {
               double t = (double)i / pathSteps;
               Vec3d sample = abovePos.lerp(targetCenter, t);
               if (this.invalid(sample)) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private boolean invalid(Vec3d pos) {
      if (this.mc.world == null) {
         return true;
      } else {
         double clampedY = MathHelper.clamp(pos.y, this.mc.world.getBottomY(), this.mc.world.getTopYInclusive() - 1);
         if (clampedY != pos.y) {
            return true;
         } else {
            BlockPos floored = BlockPos.ofFloored(pos);
            int chunkX = floored.getX() >> 4;
            int chunkZ = floored.getZ() >> 4;
            if (this.mc.world.getChunkManager().getWorldChunk(chunkX, chunkZ) == null) {
               return true;
            } else if (this.positionCache.containsKey(pos)) {
               return this.positionCache.get(pos);
            } else {
               Entity entity = this.mc.player;
               Vec3d delta = pos.subtract(entity.getEntityPos());
               Box box = entity.getBoundingBox().offset(delta);
               this.mutablePos.set(floored);

               for (int x = -1; x <= 1; x++) {
                  this.mutablePos.setX(floored.getX() + x);

                  for (int y = -1; y <= 1; y++) {
                     this.mutablePos.setY(floored.getY() + y);

                     for (int z = -1; z <= 1; z++) {
                        this.mutablePos.setZ(floored.getZ() + z);
                        BlockState state = this.mc.world.getBlockState(this.mutablePos);
                        if (state.isOf(Blocks.LAVA)
                           || state.isOf(Blocks.FIRE)
                           || state.isOf(Blocks.SOUL_FIRE)
                           || state.isOf(Blocks.MAGMA_BLOCK)
                           || state.isOf(Blocks.CAMPFIRE)
                           || state.isOf(Blocks.SWEET_BERRY_BUSH)
                           || state.isOf(Blocks.POWDER_SNOW)) {
                           this.positionCache.put(pos, true);
                           return true;
                        }
                     }
                  }
               }

               for (Entity e : this.mc.world.getOtherEntities(entity, box)) {
                  if (e.isCollidable(entity)) {
                     this.positionCache.put(pos, true);
                     return true;
                  }
               }

               boolean collides = this.mc.world.getBlockCollisions(entity, box).iterator().hasNext();
               this.positionCache.put(pos, collides);
               return collides;
            }
         }
      }
   }

   private void rotateToTarget(Entity target) {
      if (this.mc.player != null && target != null) {
         Vec3d playerPos = this.mc.player.getEyePos();
         Box box = target.getBoundingBox();
         double targetCenterY = box.getCenter().y;
         double heightDiff = targetCenterY - playerPos.y;
         double boxHeight = box.maxY - box.minY;
         double targetY;
         if (Math.abs(heightDiff) < 1.0) {
            targetY = targetCenterY;
         } else if (heightDiff > 0.0) {
            double offset = Math.min(heightDiff / 5.0, 0.4);
            targetY = targetCenterY - boxHeight * offset;
         } else {
            double offset = Math.min(-heightDiff / 5.0, 0.4);
            targetY = targetCenterY + boxHeight * offset;
         }

         Vec3d targetPos = new Vec3d(box.getCenter().x, targetY, box.getCenter().z);
         Vec3d toTarget = targetPos.subtract(playerPos).normalize();
         float yaw = (float)(Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
         float pitch = (float)(-Math.toDegrees(Math.asin(toTarget.y)));
         this.mc.player.setYaw(yaw);
         this.mc.player.setHeadYaw(yaw);
         this.mc.player.setPitch(pitch);
      }
   }

   @EventHandler
   private void onSendPacket(Send event) {
      if (Utils.canUpdate()) {
         if (event.packet instanceof PlayerMoveC2SPacket p) {
            if (this.mode.get() == SpearKill.Mode.Blink && this.isBlinking && !this.isFlushing) {
               event.cancel();
               synchronized (this.packets) {
                  if (!this.packets.isEmpty()) {
                     PlayerMoveC2SPacket last = this.packets.get(this.packets.size() - 1);
                     if (this.isSamePacket(p, last)) {
                        return;
                     }
                  }

                  this.packets.add(p);
               }
            }
         }
      }
   }

   @EventHandler
   private void onReceivePacket(Receive event) {
      if (this.mc.world != null) {
         if (event.packet instanceof PlayerPositionLookS2CPacket) {
            if (this.mode.get() == SpearKill.Mode.Blink && this.isBlinking) {
               synchronized (this.packets) {
                  this.packets.clear();
               }

               this.startPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
               this.lastTargetDistance = this.killtarget != null ? this.mc.player.distanceTo(this.killtarget) : Double.MAX_VALUE;
               this.wasApproaching = false;
            }
         }
      }
   }

   private int getReadyTicks(Item item) {
      String name = item.toString().toLowerCase();
      int value = 14;
      if (name.contains("wooden")) {
         value = 14;
      } else if (name.contains("stone") || name.contains("golden")) {
         value = 13;
      } else if (name.contains("copper")) {
         value = 12;
      } else if (name.contains("iron")) {
         value = 11;
      } else if (name.contains("diamond")) {
         value = 9;
      } else if (name.contains("netherite")) {
         value = 7;
      }

      return Math.round(value * (((Integer)this.getreadyticksModifier.get()).intValue() / 100.0F));
   }

   private void startBlink() {
      this.isBlinking = true;
      this.startPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
      synchronized (this.packets) {
         this.packets.clear();
      }

      this.lastTargetDistance = this.killtarget != null ? this.mc.player.distanceTo(this.killtarget) : Double.MAX_VALUE;
      this.wasApproaching = false;
   }

   private boolean isSamePacket(PlayerMoveC2SPacket a, PlayerMoveC2SPacket b) {
      return a.isOnGround() == b.isOnGround()
         && a.getYaw(-1.0F) == b.getYaw(-1.0F)
         && a.getPitch(-1.0F) == b.getPitch(-1.0F)
         && a.getX(-1.0) == b.getX(-1.0)
         && a.getY(-1.0) == b.getY(-1.0)
         && a.getZ(-1.0) == b.getZ(-1.0);
   }

   private void flushPackets() {
      if (this.mc.player != null && this.mc.player.networkHandler != null) {
         synchronized (this.packets) {
            if (!this.packets.isEmpty()) {
               this.isFlushing = true;
               Vec3d currentPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
               double distance = this.startPos != null ? this.startPos.distanceTo(currentPos) : 0.0;
               if (distance < (Double)this.flushRange.get()) {
                  this.packets.clear();
                  this.isFlushing = false;
               } else {
                  Vec3d sendStartPos = this.startPos;
                  double boost = (Double)this.blinkDistanceBoost.get();
                  if (boost > 0.0 && this.startPos != null) {
                     Vec3d direction = currentPos.subtract(this.startPos);
                     Vec3d horizontalDir = new Vec3d(direction.x, 0.0, direction.z).normalize();
                     if (horizontalDir.length() > 0.01) {
                        Vec3d targetPos = this.startPos.subtract(horizontalDir.multiply(boost));
                        HitResult hit = this.mc
                           .world
                           .raycast(new RaycastContext(this.startPos, targetPos, ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player));
                        if (hit.getType() == Type.MISS) {
                           sendStartPos = targetPos;
                        } else {
                           sendStartPos = hit.getPos().add(horizontalDir.multiply(0.5));
                        }
                     }
                  }

                  if (sendStartPos != null) {
                     PlayerMoveC2SPacket startPacket = new Full(
                        sendStartPos.x,
                        sendStartPos.y,
                        sendStartPos.z,
                        this.mc.player.getYaw(),
                        this.mc.player.getPitch(),
                        false,
                        false
                     );
                     this.mc.player.networkHandler.sendPacket(startPacket);
                  }

                  if (this.chatFeedback) {
                     double totalDist = sendStartPos != null ? sendStartPos.distanceTo(currentPos) : distance;
                     this.info("Flush: %.1f blocks (actual=%.1f, boost=%.1f)", new Object[]{totalDist, distance, boost});
                  }

                  PlayerMoveC2SPacket endPacket = new Full(
                     currentPos.x,
                     currentPos.y,
                     currentPos.z,
                     this.mc.player.getYaw(),
                     this.mc.player.getPitch(),
                     this.mc.player.isOnGround(),
                     this.mc.player.horizontalCollision
                  );
                  this.mc.player.networkHandler.sendPacket(endPacket);
                  this.packets.clear();
                  this.isFlushing = false;
               }
            }
         }
      }
   }

   private Entity target() {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.mc.crosshairTarget instanceof EntityHitResult hit && this.isValidTarget(hit.getEntity())) {
            return hit.getEntity();
         } else {
            double maxRange = (Double)this.maxrange.get();
            Vec3d eyePos = this.mc.player.getEyePos();
            Vec3d lookVec = this.mc.player.getRotationVec(1.0F);
            HitResult blockHit = this.mc
               .world
               .raycast(
                  new RaycastContext(
                     eyePos, eyePos.add(lookVec.multiply(maxRange)), ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player
                  )
               );
            double rayLength = blockHit.getType() == Type.MISS ? maxRange : eyePos.distanceTo(blockHit.getPos());
            List<Entity> candidates = this.mc
               .world
               .getOtherEntities(
                  this.mc.player,
                  this.mc.player.getBoundingBox().stretch(lookVec.multiply(rayLength)),
                  ex -> ex instanceof LivingEntity && ex.isAlive() && ex != this.mc.player
               );
            candidates.sort(Comparator.comparingDouble(ex -> eyePos.squaredDistanceTo(ex.getBoundingBox().getCenter())));
            double coneAngle = 0.999;

            for (Entity e : candidates) {
               double dist = eyePos.distanceTo(e.getBoundingBox().getCenter());
               if (dist > maxRange) {
                  break;
               }

               if (this.isValidTarget(e) && this.canSeeTarget(e)) {
                  Vec3d toEntity = e.getBoundingBox().getCenter().subtract(eyePos).normalize();
                  if (lookVec.dotProduct(toEntity) > coneAngle) {
                     return e;
                  }
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private boolean canSeeTarget(Entity target) {
      if (this.mc.player != null && this.mc.world != null) {
         Vec3d eyePos = this.mc.player.getEyePos();
         Vec3d targetCenter = target.getBoundingBox().getCenter();
         HitResult result = this.mc
            .world
            .raycast(new RaycastContext(eyePos, targetCenter, ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player));
         return result.getType() == Type.MISS ? true : eyePos.distanceTo(result.getPos()) >= eyePos.distanceTo(targetCenter) - 0.5;
      } else {
         return false;
      }
   }

   private boolean isValidTarget(Entity entity) {
      if (entity == null) {
         return false;
      } else if (entity instanceof PlayerEntity player && (Boolean)this.ignorefriends.get() && Friends.get().isFriend(player)) {
         return false;
      } else {
         EntityType<?> type = entity.getType();
         boolean inList = ((Set)this.targetEntities.get()).contains(type);
         return this.targetListMode.get() != SpearKill.TargetListMode.Whitelist ? !inList : inList || ((Set)this.targetEntities.get()).isEmpty();
      }
   }

   public static enum Mode {
      Lunge,
      Blink;
   }

   public static enum TargetListMode {
      Whitelist,
      Blacklist;
   }

   public static enum lungeMode {
      DirectionBased,
      FromAbove,
      Auto_FromAboveFirst;
   }
}
