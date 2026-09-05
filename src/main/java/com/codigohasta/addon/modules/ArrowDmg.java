package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class ArrowDmg extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgAuto = this.settings.createGroup("Shoot");
   private final SettingGroup sgTotem = this.settings.createGroup("Imagepast");
   private final SettingGroup sgAim = this.settings.createGroup("Aimbot");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> strength = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("power")).description("LogicupmostbigSupport10, atpaperServicecanPullmorebig"))
            .defaultValue(10.0)
            .min(0.1)
            .sliderMax(20.0)
            .build()
      );
   private final Setting<Boolean> vertical = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Vertical Correction"))
                  .description("EnableafterjustcanSelfAngleShootStrike"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> useOffset = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DefenseFall"))
                  .description("DefenseStopyourselfFalldie. like thiscanatSkyupRandomMeaningShootStrike, notwilltoFallHurt."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> smartStrength = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AutoSpaceCheckTest"))
                  .description("afterDirectionSendShootLine, ifBodyafter2high'sSpacehaveBlock, AutoshortSendPackDistance, fewnoEffectArrow."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> yeetTridents = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("TridentMode"))
                  .description("whethertoTridentalsoUsehighHurt."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> autoShoot = this.sgAuto
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("EnableShoot"))
                  .description("AutoPowerOpenShootStrike, Invincible Machine Gun."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Integer> charge = this.sgAuto
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("PowerTime"))
                     .description("PowermanyfewTickafterAutoSendShoot. highHurtModedownUse4."))
                  .defaultValue(4))
               .min(1)
               .sliderMax(20)
               .visible(this.autoShoot::get))
            .build()
      );
   private final Setting<Boolean> onlyWhenHoldingRightClick = this.sgAuto
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("RightKeyTimeShoot"))
                     .description("EnableafterOnlyhavelongRightKeythenwillShoot. DisableAutoShoot."))
                  .defaultValue(true))
               .visible(this.autoShoot::get))
            .build()
      );
   private final Setting<Boolean> totemBypass = this.sgTotem
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("ImageSendMechanism"))
                  .description("PutTimeAutoShootArrow. OneArrowBasicpowerImage, ArrowhighHurtatInvincibleinsideinstakill."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Double> bypassStrength = this.sgTotem
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Arrowpower"))
                  .description("Arrow'spowerNumber(MustthanBasicpowerhighthencanSpawnDamage),alsojustOneArrowpowerwantlowArrowpower."))
               .defaultValue(20.0)
               .min(0.1)
               .sliderMax(30.0)
               .visible(this.totemBypass::get))
            .build()
      );
   private final Setting<Integer> bypassDelay = this.sgTotem
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("ArrowDelay"))
                     .description("OneArrowShootOutafter, AutoPowermanyfewTickShootArrow(Recommend 4)."))
                  .defaultValue(4))
               .min(1)
               .sliderMax(10)
               .visible(this.totemBypass::get))
            .build()
      );
   private final Setting<Boolean> aimbot = this.sgAim
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("EnableAimbot"))
                  .description("DirectClientView, AutoLock Target."))
               .defaultValue(false))
            .build()
      );
   private final Setting<ArrowDmg.TargetPriority> priority = this.sgAim
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                        .name("Degree"))
                     .description("AimbotSelectTarget's."))
                  .defaultValue(ArrowDmg.TargetPriority.Angle))
               .visible(this.aimbot::get))
            .build()
      );
   private final Setting<Double> aimRange = this.sgAim
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Aimbot Range")).description("AutoLock Target'smostbigDistance."))
               .defaultValue(40.0)
               .min(1.0)
               .sliderMax(100.0)
               .visible(this.aimbot::get))
            .build()
      );
   private final Setting<Boolean> aimOnlyWhenHoldingRightClick = this.sgAim
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("PullTimeAimbot"))
                     .description("OnlyhaveatPrepareShootStrike(HoldRightKeyAutoShootStrike)TimethenLockView."))
                  .defaultValue(true))
               .visible(this.aimbot::get))
            .build()
      );
   private final Setting<Boolean> ignoreWalls = this.sgAim
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("afterTarget"))
                     .description("Enableafter, OnlyhaveTargetatViewLineinsideTimethenwillAimbot."))
                  .defaultValue(true))
               .visible(this.aimbot::get))
            .build()
      );
   private final Setting<Set<EntityType<?>>> entities = this.sgAim
      .add(
         ((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)new meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder()
                     .name("AimbotTarget"))
                  .description("SelectyouwantAutoAim'sEntityType."))
               .defaultValue(new EntityType[]{EntityType.PLAYER})
               .visible(this.aimbot::get))
            .build()
      );
   private final Setting<Boolean> doRender = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("EnableRender"))
                  .description("EnableThingLogicPreTestLineandTargetBoxRender."))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> boxColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("TargetBoxColor"))
                  .description("byLockAim Target'sRenderBoxColor."))
               .defaultValue(new SettingColor(255, 0, 0, 100))
               .visible(this.doRender::get))
            .build()
      );
   private boolean isShooting = false;
   private boolean forcedPressed = false;
   private Entity currentTarget = null;
   private boolean isSecondShot = false;
   private int bypassTimer = -1;

   public ArrowDmg() {
      super(AddonTemplate.CATEGORY, "ArrowDmg", "Calculates and boosts arrow / trident projectile damage with configurable power and trajectory correction.");
   }

   public void onDeactivate() {
      if (this.forcedPressed) {
         this.mc.options.useKey.setPressed(false);
         this.forcedPressed = false;
      }

      this.currentTarget = null;
      this.isSecondShot = false;
      this.bypassTimer = -1;
   }

   @EventHandler
   private void onTick(Pre event) {
      if ((Boolean)this.totemBypass.get() && this.bypassTimer > 0) {
         this.mc.options.useKey.setPressed(true);
         this.bypassTimer--;
         if (this.bypassTimer == 0) {
            if (this.mc.player.isUsingItem() && this.isValidItem(this.mc.player.getActiveItem())) {
               this.mc.interactionManager.stopUsingItem(this.mc.player);
            } else {
               this.isSecondShot = false;
            }

            this.mc.options.useKey.setPressed(false);
         }
      } else if (this.mc.player != null && this.mc.world != null) {
         boolean validMainHand = this.isValidItem(this.mc.player.getMainHandStack());
         boolean validOffHand = this.isValidItem(this.mc.player.getOffHandStack());
         boolean hasValidItem = validMainHand || validOffHand;
         this.currentTarget = null;
         if ((Boolean)this.aimbot.get() && hasValidItem) {
            boolean isPressingRightClick = this.mc.options.useKey.isPressed() || this.forcedPressed;
            if (!(Boolean)this.aimOnlyWhenHoldingRightClick.get() || isPressingRightClick) {
               Entity bestTarget = null;
               double bestScore = Double.MAX_VALUE;

               for (Entity entity : this.mc.world.getEntities()) {
                  if (entity != this.mc.player
                     && entity instanceof LivingEntity living
                     && !living.isDead()
                     && !(living.getHealth() <= 0.0F)
                     && ((Set)this.entities.get()).contains(entity.getType())
                     && !(entity instanceof PlayerEntity player && (player.isCreative() || player.isSpectator() || Friends.get().isFriend(player)))) {
                     double dist = this.mc.player.distanceTo(entity);
                     if (!(dist > (Double)this.aimRange.get()) && (!(Boolean)this.ignoreWalls.get() || this.mc.player.canSee(entity))) {
                        double score = 0.0;
                        switch ((ArrowDmg.TargetPriority)this.priority.get()) {
                           case Angle:
                              Vec3d targetPos = entity.getBoundingBox().getCenter();
                              double dX = targetPos.x - this.mc.player.getX();
                              double dY = targetPos.y - this.mc.player.getEyeY();
                              double dZ = targetPos.z - this.mc.player.getZ();
                              double yawDiff = Math.toDegrees(Math.atan2(dZ, dX)) - 90.0 - this.mc.player.getYaw();
                              double pitchDiff = -Math.toDegrees(Math.atan2(dY, Math.sqrt(dX * dX + dZ * dZ))) - this.mc.player.getPitch();
                              score = Math.abs(MathHelper.wrapDegrees((float)yawDiff)) + Math.abs(MathHelper.wrapDegrees((float)pitchDiff));
                              break;
                           case Distance:
                              score = dist;
                              break;
                           case Health:
                              score = living.getHealth();
                        }

                        if (score < bestScore) {
                           bestScore = score;
                           bestTarget = entity;
                        }
                     }
                  }
               }

               if (bestTarget != null) {
                  this.currentTarget = bestTarget;
                  Vec3d targetPos = bestTarget.getBoundingBox().getCenter();
                  double dX = targetPos.x - this.mc.player.getX();
                  double dY = targetPos.y - this.mc.player.getEyeY();
                  double dZ = targetPos.z - this.mc.player.getZ();
                  double distXZ = Math.sqrt(dX * dX + dZ * dZ);
                  float yaw = (float)Math.toDegrees(Math.atan2(dZ, dX)) - 90.0F;
                  float pitch = (float)(-Math.toDegrees(Math.atan2(dY, distXZ)));
                  pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
                  this.mc.player.setYaw(yaw);
                  this.mc.player.setPitch(pitch);
               }
            }
         }

         ItemStack activeStack = this.mc.player.getActiveItem();
         if (this.mc.player.isUsingItem() && !this.isValidItem(activeStack)) {
            if (this.forcedPressed) {
               this.mc.options.useKey.setPressed(false);
               this.forcedPressed = false;
            }
         } else if ((Boolean)this.autoShoot.get() && hasValidItem) {
            if (!(Boolean)this.onlyWhenHoldingRightClick.get() && !this.mc.player.isUsingItem()) {
               this.mc.options.useKey.setPressed(true);
               this.forcedPressed = true;
            }

            if (this.mc.player.isUsingItem() && this.isValidItem(activeStack) && this.mc.player.getItemUseTime() >= (Integer)this.charge.get()) {
               this.mc.interactionManager.stopUsingItem(this.mc.player);
            }
         } else {
            if (this.forcedPressed) {
               this.mc.options.useKey.setPressed(false);
               this.forcedPressed = false;
            }
         }
      }
   }

   @EventHandler
   private void onSendPacket(Send event) {
      if (!this.isShooting) {
         if (event.packet instanceof PlayerActionC2SPacket packet
            && packet.getAction() == Action.RELEASE_USE_ITEM
            && this.mc.player != null
            && (this.isValidItem(this.mc.player.getMainHandStack()) || this.isValidItem(this.mc.player.getOffHandStack()))) {
            event.cancel();
            this.processShoot(packet);
         }
      }
   }

   private void processShoot(PlayerActionC2SPacket releasePacket) {
      if (this.mc.player != null && this.mc.world != null && this.mc.getNetworkHandler() != null) {
         this.isShooting = true;
         this.mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(this.mc.player, Mode.START_SPRINTING));
         double x = this.mc.player.getX();
         double y = this.mc.player.getY();
         double z = this.mc.player.getZ();
         double currentStrength = this.isSecondShot && this.totemBypass.get() ? (Double)this.bypassStrength.get() : (Double)this.strength.get();
         double adjustedStrength = currentStrength / 10.0 * Math.sqrt(500.0);
         Vec3d lookVec = this.mc.player.getRotationVector().multiply(adjustedStrength);
         Vec3d spoofOffset = new Vec3d(-lookVec.x, this.vertical.get() ? -lookVec.y : 0.0, -lookVec.z);
         if ((Boolean)this.smartStrength.get()) {
            double safeDist = this.getSafeSpoofDistance(new Vec3d(x, y, z), spoofOffset);
            double adjustedDist = Math.max(0.01, safeDist - 0.5);
            if (adjustedDist < spoofOffset.length()) {
               spoofOffset = spoofOffset.normalize().multiply(adjustedDist);
            }
         }

         double targetX = x + spoofOffset.x;
         double targetY = y + spoofOffset.y;
         double targetZ = z + spoofOffset.z;

         for (int i = 0; i < 4; i++) {
            this.sendPos(x, y, z, true);
         }

         this.sendPos(targetX, targetY, targetZ, false);
         this.sendPos(x, y, z, false);
         this.mc.getNetworkHandler().sendPacket(releasePacket);
         if ((Boolean)this.vertical.get() && (Boolean)this.useOffset.get() && spoofOffset.y > 0.0) {
            this.sendPos(x, y + 0.01, z, false);
         }

         this.isShooting = false;
         if ((Boolean)this.totemBypass.get()) {
            if (!this.isSecondShot) {
               this.isSecondShot = true;
               this.bypassTimer = (Integer)this.bypassDelay.get();
            } else {
               this.isSecondShot = false;
            }
         } else {
            this.isSecondShot = false;
         }
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if ((Boolean)this.doRender.get() && this.mc.player != null && this.mc.world != null) {
         if (this.isValidItem(this.mc.player.getMainHandStack()) || this.isValidItem(this.mc.player.getOffHandStack())) {
            float tickDelta = event.tickDelta;
            float pitchInterp = MathHelper.lerp(tickDelta, this.mc.player.lastPitch, this.mc.player.getPitch());
            float yawInterp = MathHelper.lerp(tickDelta, this.mc.player.lastYaw, this.mc.player.getYaw());
            float radPitch = pitchInterp * (float) (Math.PI / 180.0);
            float radYaw = -yawInterp * (float) (Math.PI / 180.0);
            float cosYaw = MathHelper.cos(radYaw);
            float sinYaw = MathHelper.sin(radYaw);
            float cosPitch = MathHelper.cos(radPitch);
            float sinPitch = MathHelper.sin(radPitch);
            Vec3d lookVec = new Vec3d(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
            double baseStr = (Double)this.strength.get() / 10.0 * Math.sqrt(500.0);
            Vec3d spoofOffset = new Vec3d(
               -lookVec.x * baseStr, this.vertical.get() ? -lookVec.y * baseStr : 0.0, -lookVec.z * baseStr
            );
            double maxD = spoofOffset.length();
            double finalVelAdd = maxD;
            Color laserColor = new Color(0, 255, 0, 255);
            if ((Boolean)this.smartStrength.get()) {
               double sDist = this.getSafeSpoofDistance(
                  new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ()), spoofOffset
               );
               double adjDist = Math.max(0.01, sDist - 0.5);
               if (adjDist < maxD) {
                  finalVelAdd = adjDist;
               }

               float ratio = (float)MathHelper.clamp(sDist / maxD, 0.0, 1.0);
               laserColor = new Color((int)((1.0F - ratio) * 255.0F), (int)(ratio * 255.0F), 0, 255);
            }

            double renderX = MathHelper.lerp(tickDelta, this.mc.player.lastX, this.mc.player.getX());
            double renderY = MathHelper.lerp(tickDelta, this.mc.player.lastY, this.mc.player.getY())
               + (this.mc.player.getEyeY() - this.mc.player.getY());
            double renderZ = MathHelper.lerp(tickDelta, this.mc.player.lastZ, this.mc.player.getZ());
            Vec3d simPos = new Vec3d(renderX, renderY - 0.1, renderZ);
            Vec3d simVel = lookVec.normalize().multiply(3.0 + finalVelAdd);
            List<Vec3d> points = new ArrayList<>();
            points.add(simPos);
            Entity hitEnt = null;

            for (int step = 0; step < 150; step++) {
               Vec3d nextSimPos = simPos.add(simVel);
               RaycastContext bCtx = new RaycastContext(simPos, nextSimPos, ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player);
               HitResult bHit = this.mc.world.raycast(bCtx);
               if (bHit != null && bHit.getType() == Type.BLOCK) {
                  nextSimPos = bHit.getPos();
               }

               Box segBox = new Box(
                     simPos.x, simPos.y, simPos.z, nextSimPos.x, nextSimPos.y, nextSimPos.z
                  )
                  .expand(0.5);
               double nearest = Double.MAX_VALUE;

               for (Entity e : this.mc.world.getOtherEntities(this.mc.player, segBox)) {
                  if (e instanceof LivingEntity living
                     && living.isAlive()
                     && !(e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator() || Friends.get().isFriend(p)))) {
                     Optional<Vec3d> clip = e.getBoundingBox().expand(0.3).raycast(simPos, nextSimPos);
                     if (clip.isPresent()) {
                        double d = simPos.squaredDistanceTo(clip.get());
                        if (d < nearest) {
                           nearest = d;
                           nextSimPos = clip.get();
                           hitEnt = e;
                        }
                     }
                  }
               }

               points.add(nextSimPos);
               if (hitEnt != null || bHit != null && bHit.getType() == Type.BLOCK) {
                  break;
               }

               simPos = nextSimPos;
               simVel = simVel.multiply(0.99).subtract(0.0, 0.05, 0.0);
            }

            if (points.size() >= 2) {
               Vec3d pStart = points.get(0);
               Vec3d pNext = points.get(1);
               if (pStart.distanceTo(pNext) > 8.0) {
                  Vec3d dir = pNext.subtract(pStart).normalize();
                  points.set(0, pStart.add(dir.multiply(1.5)));
               }
            }

            for (int renderIdx = 0; renderIdx < points.size() - 1; renderIdx++) {
               Vec3d p1 = points.get(renderIdx);
               Vec3d p2 = points.get(renderIdx + 1);
               event.renderer.line(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, laserColor);
            }

            if (hitEnt != null) {
               event.renderer.box(hitEnt.getBoundingBox(), (Color)this.boxColor.get(), (Color)this.boxColor.get(), ShapeMode.Lines, 0);
            }
         }
      }
   }

   private double getSafeSpoofDistance(Vec3d start, Vec3d offset) {
      Vec3d end = start.add(offset);
      double maxDist = offset.length();
      RaycastContext footContext = new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player);
      HitResult footHit = this.mc.world.raycast(footContext);
      Vec3d headOffsetVec = new Vec3d(0.0, 1.8, 0.0);
      Vec3d headStart = start.add(headOffsetVec);
      Vec3d headEnd = end.add(headOffsetVec);
      RaycastContext headContext = new RaycastContext(headStart, headEnd, ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player);
      HitResult headHit = this.mc.world.raycast(headContext);
      double safeDist = maxDist;
      if (footHit != null && footHit.getType() == Type.BLOCK) {
         safeDist = Math.min(maxDist, start.distanceTo(footHit.getPos()));
      }

      if (headHit != null && headHit.getType() == Type.BLOCK) {
         safeDist = Math.min(safeDist, headStart.distanceTo(headHit.getPos()));
      }

      return safeDist;
   }

   private void sendPos(double x, double y, double z, boolean onGround) {
      this.mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y, z, onGround, this.mc.player.horizontalCollision));
   }

   private boolean isValidItem(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         String name = stack.getItem().toString();
         return name.contains("bow") || (Boolean)this.yeetTridents.get() && name.contains("trident");
      } else {
         return false;
      }
   }

   public static enum TargetPriority {
      Angle,
      Distance,
      Health;
   }
}
