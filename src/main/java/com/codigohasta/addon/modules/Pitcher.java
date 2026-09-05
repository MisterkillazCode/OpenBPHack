package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class Pitcher extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgTP = this.settings.createGroup("TPSetting(copy in Trouser-Streak");
   private final SettingGroup sgAuto = this.settings.createGroup("Shoot");
   private final SettingGroup sgTotem = this.settings.createGroup("Imagepast");
   private final SettingGroup sgAim = this.settings.createGroup("Aimbot");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Pitcher.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("TolerateMode")).description("Vanilla = 22, Paper = mosthigh149")).defaultValue(Pitcher.Mode.Paper))
            .build()
      );
   private final Setting<Pitcher.TPMode> tpmode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("TPDirection")).description("afterDamagemorehigh, beforeDistancemore"))
               .defaultValue(Pitcher.TPMode.Reverse))
            .build()
      );
   private final Setting<List<Item>> projectileItems = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
               .name("UseItem"))
            .defaultValue(new Item[]{Items.BOW, Items.TRIDENT, Items.ENDER_PEARL, Items.SPLASH_POTION, Items.EXPERIENCE_BOTTLE, Items.SNOWBALL})
            .build()
      );
   private final Setting<Double> paperDistance = this.sgTP
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("PapermostbigDistance"))
               .defaultValue(149.0)
               .sliderMax(169.0)
               .visible(() -> this.mode.get() == Pitcher.Mode.Paper))
            .build()
      );
   private final Setting<Integer> paperPackets = this.sgTP
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("PaperPackNumber"))
                  .defaultValue(15))
               .min(1)
               .sliderMax(20)
               .visible(() -> this.mode.get() == Pitcher.Mode.Paper))
            .build()
      );
   private final Setting<Double> strength = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("power"))
               .description("LogicupmostbigSupport10, atpaperServicecanPullmorebig"))
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
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("Arrowpower"))
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
   private final Setting<Pitcher.TargetPriority> priority = this.sgAim
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("Degree")).description("AimbotSelectTarget's.")).defaultValue(Pitcher.TargetPriority.Angle))
               .visible(this.aimbot::get))
            .build()
      );
   private final Setting<Double> aimRange = this.sgAim
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("Aimbot Range"))
                  .description("AutoLock Target'smostbigDistance."))
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
   private int totemStep = 0;
   private int bypassTimer = -1;

   public Pitcher() {
      super(AddonTemplate.CATEGORY, "Pitcher", "Aims and throws projectiles (snowballs, eggs, splash potions) at a target with damage calculation.");
   }

   public void onDeactivate() {
      if (this.forcedPressed) {
         this.mc.options.useKey.setPressed(false);
         this.forcedPressed = false;
      }

      this.totemStep = 0;
      this.bypassTimer = -1;
      this.isShooting = false;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         boolean validMain = this.isValidItem(this.mc.player.getMainHandStack());
         boolean validOff = this.isValidItem(this.mc.player.getOffHandStack());
         boolean hasValidItem = validMain || validOff;
         Hand hand = validMain ? Hand.MAIN_HAND : Hand.OFF_HAND;
         if ((Boolean)this.totemBypass.get() && this.totemStep == 1) {
            if (this.bypassTimer == (Integer)this.bypassDelay.get()) {
               this.mc.interactionManager.interactItem(this.mc.player, hand);
            }

            if (this.bypassTimer > 0) {
               this.mc.options.useKey.setPressed(true);
               this.bypassTimer--;
            } else if (this.bypassTimer == 0) {
               this.mc.interactionManager.stopUsingItem(this.mc.player);
               this.mc.options.useKey.setPressed(false);
               this.totemStep = 0;
               this.bypassTimer = -1;
            }
         } else {
            if ((Boolean)this.aimbot.get() && hasValidItem) {
               this.currentTarget = null;
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
                           switch ((Pitcher.TargetPriority)this.priority.get()) {
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
                     this.mc.player.setYaw((float)Math.toDegrees(Math.atan2(dZ, dX)) - 90.0F);
                     this.mc.player.setPitch((float)(-Math.toDegrees(Math.atan2(dY, distXZ))));
                  }
               }
            }

            if (this.totemStep == 0) {
               this.handleNormalAutoShoot(hasValidItem);
            }
         }
      }
   }

   private void handleNormalAutoShoot(boolean hasValidItem) {
      if ((Boolean)this.autoShoot.get() && hasValidItem) {
         ItemStack activeStack = this.mc.player.getActiveItem();
         if (this.mc.player.isUsingItem() && !this.isValidItem(activeStack)) {
            if (this.forcedPressed) {
               this.mc.options.useKey.setPressed(false);
               this.forcedPressed = false;
            }
         } else {
            if (!(Boolean)this.onlyWhenHoldingRightClick.get() && !this.mc.player.isUsingItem()) {
               this.mc.options.useKey.setPressed(true);
               this.forcedPressed = true;
            }

            if (this.mc.player.isUsingItem() && this.isValidItem(activeStack) && this.mc.player.getItemUseTime() >= (Integer)this.charge.get()) {
               this.mc.interactionManager.stopUsingItem(this.mc.player);
            }
         }
      }
   }

   @EventHandler
   private void onSendPacket(Send event) {
      if (!this.isShooting && this.mc.player != null) {
         if (event.packet instanceof PlayerActionC2SPacket p && p.getAction() == Action.RELEASE_USE_ITEM) {
            if (this.isValidProjectile(this.mc.player.getActiveItem())) {
               event.cancel();
               this.processShoot(event.packet);
            }
         } else if (event.packet instanceof PlayerInteractItemC2SPacket px) {
            ItemStack stack = px.getHand() == Hand.MAIN_HAND ? this.mc.player.getMainHandStack() : this.mc.player.getOffHandStack();
            if (this.isValidProjectile(stack)) {
               event.cancel();
               this.processShoot(event.packet);
            }
         }
      }
   }

   private void processShoot(Packet<?> originalPacket) {
      if (this.mc.player != null && this.mc.getNetworkHandler() != null) {
         this.isShooting = true;
         double currentStr = this.totemStep == 1 ? (Double)this.bypassStrength.get() : (Double)this.strength.get();
         this.mc
            .getNetworkHandler()
            .sendPacket(new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_SPRINTING));
         double x = this.mc.player.getX();
         double y = this.mc.player.getY();
         double z = this.mc.player.getZ();
         Vec3d startPos = new Vec3d(x, y, z);
         Vec3d lookVec = this.mc.player.getRotationVector();
         currentStr = this.isSecondShot && this.totemBypass.get() ? (Double)this.bypassStrength.get() : (Double)this.strength.get();
         double maxDist = this.mode.get() == Pitcher.Mode.Vanilla ? 21.9 : (Double)this.paperDistance.get();
         double adjustedStrength = currentStr / 10.0 * Math.sqrt(500.0);
         adjustedStrength = Math.min(adjustedStrength, maxDist);
         Vec3d dir = this.tpmode.get() == Pitcher.TPMode.Reverse ? lookVec.multiply(-1.0) : lookVec;
         Vec3d spoofOffset = new Vec3d(
            dir.x * adjustedStrength, this.vertical.get() ? dir.y * adjustedStrength : 0.0, dir.z * adjustedStrength
         );
         if ((Boolean)this.smartStrength.get()) {
            double safeDist = this.getSafeSpoofDistance(startPos, spoofOffset);
            double adjustedDist = Math.max(0.01, safeDist - 0.5);
            if (adjustedDist < spoofOffset.length()) {
               spoofOffset = spoofOffset.normalize().multiply(adjustedDist);
            }
         }

         Vec3d targetPos = startPos.add(spoofOffset);
         int spam = this.mode.get() == Pitcher.Mode.Vanilla ? 4 : (Integer)this.paperPackets.get();

         for (int i = 0; i < spam; i++) {
            this.mc.player.networkHandler.sendPacket(new OnGroundOnly(true, this.mc.player.horizontalCollision));
         }

         this.sendMovePacket(targetPos);
         if (this.tpmode.get() == Pitcher.TPMode.Forward) {
            this.mc.getNetworkHandler().sendPacket(originalPacket);
         }

         this.sendMovePacket(startPos);
         if (this.tpmode.get() == Pitcher.TPMode.Reverse) {
            this.mc.getNetworkHandler().sendPacket(originalPacket);
         }

         Vec3d syncPos = startPos.add((Math.random() - 0.5) * 0.05, 0.001, (Math.random() - 0.5) * 0.05);
         this.sendMovePacket(syncPos);
         this.mc.player.setPosition(syncPos.x, syncPos.y, syncPos.z);
         if ((Boolean)this.vertical.get() && (Boolean)this.useOffset.get() && spoofOffset.y > 0.0) {
            this.sendMovePacket(startPos.add(0.0, 0.01, 0.0));
         }

         this.isShooting = false;
         if ((Boolean)this.totemBypass.get()) {
            if (this.totemStep == 0) {
               this.totemStep = 1;
               this.bypassTimer = (Integer)this.bypassDelay.get();
            } else {
               this.totemStep = 0;
               this.bypassTimer = -1;
            }
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

   private void sendMovePacket(Vec3d pos) {
      if (this.mc.getNetworkHandler() != null) {
         PlayerMoveC2SPacket packet = new PositionAndOnGround(pos.x, pos.y, pos.z, false, this.mc.player.horizontalCollision);
         ((IPlayerMoveC2SPacket)packet).meteor$setTag(1337);
         this.mc.player.networkHandler.sendPacket(packet);
      }
   }

   private boolean isValidProjectile(ItemStack stack) {
      return stack != null && !stack.isEmpty() ? ((List)this.projectileItems.get()).contains(stack.getItem()) : false;
   }

   public static enum Mode {
      Vanilla,
      Paper;
   }

   public static enum TPMode {
      Reverse,
      Forward;
   }

   public static enum TargetPriority {
      Angle,
      Distance,
      Health;
   }
}
