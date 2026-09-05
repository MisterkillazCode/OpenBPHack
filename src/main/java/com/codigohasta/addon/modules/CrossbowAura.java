package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class CrossbowAura extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgTargets = this.settings.createGroup("TargetSelect");
   private final SettingGroup sgWhitelist = this.settings.createGroup("NameSingleSetting");
   private final SettingGroup sgCrossbow = this.settings.createGroup("Setting");
   private final SettingGroup sgPrediction = this.settings.createGroup("PredictSetting");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> range = this.sgGeneral.add(((Builder)new Builder().name("Shoot")).defaultValue(60.0).min(5.0).max(150.0).build());
   private final Setting<CrossbowAura.AimMode> aimMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("AimDirection"))
               .defaultValue(CrossbowAura.AimMode.Silent))
            .build()
      );
   private final Setting<Boolean> autoSwitch = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("AutoSwitchGun"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> autoFire = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("AutoOpen"))
               .defaultValue(true))
            .build()
      );
   private final Setting<CrossbowAura.PriorityMode> priority = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Direction"))
                  .description("Distance/HealthModedownAutoAttack. Select'AttackLock'Time, MustUseinKeySelectSetTarget."))
               .defaultValue(CrossbowAura.PriorityMode.Closest))
            .build()
      );
   private final Setting<Boolean> middleClickLock = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("inKeyEnemy"))
                  .description("AllowUseinKeySettingLock Target."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> resetOnMiss = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("AirStrike"))
                     .description("toAirAirinKeyTimeDisappearLock."))
                  .defaultValue(true))
               .visible(this.middleClickLock::get))
            .build()
      );
   private final Setting<Set<EntityType<?>>> entities = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)new meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder()
               .name("Target Entity"))
            .defaultValue(new EntityType[]{EntityType.PLAYER, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER})
            .build()
      );
   private final Setting<Boolean> ignoreFriends = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("friend"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> ignorePets = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Thing"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> ignoreNamed = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("LifeNameMob"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> targetSurvival = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("AttackSpawnPlayer"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> targetCreative = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("AttackBuildPlayer"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> targetAdventure = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("AttackPlayer"))
               .defaultValue(true))
            .build()
      );
   private final Setting<CrossbowAura.ListMode> listMode = this.sgWhitelist
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("NameSingleMode"))
                  .description("WhiteNameSingle/BlackNameSingleMode."))
               .defaultValue(CrossbowAura.ListMode.Off))
            .build()
      );
   private final Setting<String> playerList = this.sgWhitelist
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                        .name("Player List"))
                     .description("PlayerIDList, UseText(,)."))
                  .defaultValue(""))
               .visible(() -> this.listMode.get() != CrossbowAura.ListMode.Off))
            .build()
      );
   private final Setting<CrossbowAura.CrossbowMode> cbMode = this.sgCrossbow
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("InstallMode"))
               .defaultValue(CrossbowAura.CrossbowMode.Native))
            .build()
      );
   private final Setting<Integer> delay = this.sgCrossbow
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("ShootStrikeDelay"))
               .defaultValue(0))
            .min(0)
            .max(10)
            .build()
      );
   private final Setting<Integer> tolerance = this.sgCrossbow
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Load Tolerance"))
                  .defaultValue(6))
               .min(0)
               .max(50)
               .visible(() -> this.cbMode.get() != CrossbowAura.CrossbowMode.Native))
            .build()
      );
   private final Setting<Boolean> predictMovement = this.sgPrediction
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("MovePredict"))
                  .description("TargetSpeedandDistanceEarlyAmount."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> calculateAcceleration = this.sgPrediction
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Speed ()"))
                     .description("PreTestTarget's. thisaretoStop//Start'sKey!"))
                  .defaultValue(true))
               .visible(this.predictMovement::get))
            .build()
      );
   private final Setting<Boolean> pingCompensation = this.sgPrediction
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Delay (Ping)"))
                     .description("Delay."))
                  .defaultValue(true))
               .visible(this.predictMovement::get))
            .build()
      );
   private final Setting<Double> predictionScale = this.sgPrediction
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("PredictRate")).description("EarlyAmount. ifArrowatTargetBodyafter, high."))
               .defaultValue(1.0)
               .min(0.0)
               .max(2.0)
               .sliderMax(1.5)
               .visible(this.predictMovement::get))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("Render Mode"))
               .defaultValue(ShapeMode.Lines))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("NormalTarget Color"))
            .defaultValue(new SettingColor(255, 0, 0, 40))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("NormalLineColor"))
            .defaultValue(new SettingColor(255, 0, 0, 200))
            .build()
      );
   private final Setting<SettingColor> lockSideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("LockTarget Color"))
            .defaultValue(new SettingColor(0, 255, 0, 80))
            .build()
      );
   private final Setting<SettingColor> lockLineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("LockLineColor"))
            .defaultValue(new SettingColor(0, 255, 0, 255))
            .build()
      );
   private final Setting<Boolean> renderPredict = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DisplayPredictpoint"))
                  .description("RenderPredict'sStrikeinPosition(CyanBox)."))
               .defaultValue(true))
            .build()
      );
   private Entity currentTarget;
   private Entity lockedTarget;
   private Vec3d lastPredictedPos = null;
   private int timer;
   private final Map<Integer, Vec3d> prevVelocities = new HashMap<>();

   public CrossbowAura() {
      super(AddonTemplate.CATEGORY, "CrossbowAura", "Auto-aims and fires crossbows at targets. Feature module.");
   }

   public void onActivate() {
      this.timer = 0;
      this.currentTarget = null;
      this.lastPredictedPos = null;
      this.prevVelocities.clear();
      if (this.priority.get() != CrossbowAura.PriorityMode.LockOnly) {
         this.lockedTarget = null;
      }
   }

   public void onDeactivate() {
      this.mc.options.useKey.setPressed(false);
      if (this.mc.player != null) {
         this.mc.interactionManager.stopUsingItem(this.mc.player);
      }

      this.lockedTarget = null;
      this.lastPredictedPos = null;
      this.prevVelocities.clear();
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.currentTarget = this.findTarget();
         this.lastPredictedPos = null;
         ItemStack mainStack = this.mc.player.getMainHandStack();
         ItemStack offStack = this.mc.player.getOffHandStack();
         boolean mainValid = this.isValidWeapon(mainStack.getItem());
         boolean offValid = this.isValidWeapon(offStack.getItem());
         if (this.currentTarget != null && (Boolean)this.autoSwitch.get() && !mainValid && !offValid) {
            FindItemResult weapon = this.findWeapon();
            if (weapon.found()) {
               InvUtils.swap(weapon.slot(), true);
               mainStack = this.mc.player.getMainHandStack();
               mainValid = this.isValidWeapon(mainStack.getItem());
            }
         }

         ItemStack activeStack;
         Hand activeHand;
         if (mainValid) {
            activeHand = Hand.MAIN_HAND;
            activeStack = mainStack;
         } else {
            if (!offValid) {
               this.currentTarget = null;
               this.prevVelocities.clear();
               return;
            }

            activeHand = Hand.OFF_HAND;
            activeStack = offStack;
         }

         boolean shouldShoot = (Boolean)this.autoFire.get() || this.mc.options.useKey.isPressed();
         if (this.currentTarget != null) {
            double dist = this.mc.player.distanceTo(this.currentTarget);
            float[] rots = this.solveBallistic(this.currentTarget, activeStack.getItem(), dist);
            Vec3d currentVel = new Vec3d(
               this.currentTarget.getX() - this.currentTarget.lastRenderX,
               this.currentTarget.getY() - this.currentTarget.lastRenderY,
               this.currentTarget.getZ() - this.currentTarget.lastRenderZ
            );
            this.prevVelocities.put(this.currentTarget.getId(), currentVel);
            if (rots != null) {
               if (this.aimMode.get() == CrossbowAura.AimMode.Lock) {
                  this.mc.player.setYaw(rots[0]);
                  this.mc.player.setPitch(rots[1]);
               }

               if (shouldShoot || this.aimMode.get() == CrossbowAura.AimMode.Lock) {
                  Rotations.rotate(rots[0], rots[1], 100, () -> {
                     if (shouldShoot) {
                        this.handleShooting(activeStack, activeHand, dist);
                     }
                  });
               }
            }
         } else {
            this.prevVelocities.clear();
            if ((Boolean)this.autoFire.get() && this.mc.options.useKey.isPressed()) {
               this.mc.options.useKey.setPressed(false);
               this.mc.interactionManager.stopUsingItem(this.mc.player);
            }
         }
      }
   }

   private Entity findTarget() {
      if (this.priority.get() == CrossbowAura.PriorityMode.LockOnly) {
         return this.lockedTarget != null && this.isValid(this.lockedTarget) ? this.lockedTarget : null;
      } else {
         List<Entity> candidates = new ArrayList<>();

         for (Entity entity : this.mc.world.getEntities()) {
            if (this.isValid(entity)) {
               candidates.add(entity);
            }
         }

         if (candidates.isEmpty()) {
            return null;
         } else {
            candidates.sort(this::compareTargets);
            return candidates.get(0);
         }
      }
   }

   private boolean isValid(Entity entity) {
      if (entity == null) {
         return false;
      } else if (entity instanceof LivingEntity && entity != this.mc.player && entity.isAlive()) {
         if (this.mc.player.distanceTo(entity) > (Double)this.range.get()) {
            return false;
         } else if (!((Set)this.entities.get()).contains(entity.getType())) {
            return false;
         } else {
            if (entity instanceof PlayerEntity player) {
               if (player.isCreative() && !(Boolean)this.targetCreative.get()) {
                  return false;
               }

               if (!player.isCreative() && !player.isSpectator() && !(Boolean)this.targetSurvival.get() && !(Boolean)this.targetAdventure.get()) {
                  return false;
               }

               GameMode gm = this.getGameMode(player);
               if (gm == GameMode.SURVIVAL && !(Boolean)this.targetSurvival.get()) {
                  return false;
               }

               if (gm == GameMode.ADVENTURE && !(Boolean)this.targetAdventure.get()) {
                  return false;
               }

               if (gm == GameMode.SPECTATOR) {
                  return false;
               }

               if ((Boolean)this.ignoreFriends.get() && !Friends.get().shouldAttack(player)) {
                  return false;
               }

               if (this.listMode.get() != CrossbowAura.ListMode.Off) {
                  String name = player.getName().getString();
                  List<String> validNames = Arrays.stream(((String)this.playerList.get()).split(",")).map(String::trim).collect(Collectors.toList());
                  switch ((CrossbowAura.ListMode)this.listMode.get()) {
                     case Whitelist:
                        if (!validNames.contains(name)) {
                           return false;
                        }
                        break;
                     case Blacklist:
                        if (validNames.contains(name)) {
                           return false;
                        }
                  }
               }
            } else {
               if ((Boolean)this.ignoreNamed.get() && entity.hasCustomName()) {
                  return false;
               }

               if ((Boolean)this.ignorePets.get() && this.isPet(entity)) {
                  return false;
               }
            }

            return this.mc.player.canSee(entity);
         }
      } else {
         return false;
      }
   }

   private int compareTargets(Entity a, Entity b) {
      return switch ((CrossbowAura.PriorityMode)this.priority.get()) {
         case LowestHealth -> Float.compare(((LivingEntity)a).getHealth(), ((LivingEntity)b).getHealth());
         default -> Double.compare(this.mc.player.distanceTo(a), this.mc.player.distanceTo(b));
      };
   }

   private void handleShooting(ItemStack stack, Hand hand, double distance) {
      if (this.timer <= 0) {
         Item item = stack.getItem();
         if (item instanceof CrossbowItem) {
            this.handleCrossbow(stack, hand);
         } else if (item instanceof BowItem) {
            this.handleBow(hand, distance);
         } else if (item == Items.TRIDENT) {
            this.handleTrident(hand);
         } else {
            this.mc.interactionManager.interactItem(this.mc.player, hand);
            this.mc.player.swingHand(hand);
            this.timer = (Integer)this.delay.get();
         }
      } else {
         this.timer--;
         if (this.cbMode.get() == CrossbowAura.CrossbowMode.Native || (Boolean)this.autoFire.get()) {
            this.mc.options.useKey.setPressed(false);
         }
      }
   }

   private void handleCrossbow(ItemStack stack, Hand hand) {
      switch ((CrossbowAura.CrossbowMode)this.cbMode.get()) {
         case Native:
            if (CrossbowItem.isCharged(stack)) {
               this.mc.interactionManager.interactItem(this.mc.player, hand);
               this.mc.player.swingHand(hand);
               this.timer = (Integer)this.delay.get();
            } else {
               this.mc.options.useKey.setPressed(true);
               if (!this.mc.player.isUsingItem()) {
                  this.mc.interactionManager.interactItem(this.mc.player, hand);
               }
            }
            break;
         case Control:
            if (CrossbowItem.isCharged(stack)) {
               this.mc.interactionManager.interactItem(this.mc.player, hand);
               this.mc.player.swingHand(hand);
               this.timer = (Integer)this.delay.get();
               return;
            }

            this.mc.options.useKey.setPressed(true);
            if (!this.mc.player.isUsingItem()) {
               this.mc.interactionManager.interactItem(this.mc.player, hand);
               return;
            }

            int time = this.getPullTime(stack) + (Integer)this.tolerance.get();
            if (this.mc.player.getItemUseTime() >= time) {
               this.mc.interactionManager.stopUsingItem(this.mc.player);
            }
            break;
         case Packet:
            if (CrossbowItem.isCharged(stack)) {
               this.mc.interactionManager.interactItem(this.mc.player, hand);
               this.mc.player.swingHand(hand);
               this.timer = (Integer)this.delay.get();
            }

            if (!this.mc.player.isUsingItem()) {
               this.mc.interactionManager.interactItem(this.mc.player, hand);
               this.mc.options.useKey.setPressed(true);
               return;
            }

            this.mc.options.useKey.setPressed(true);
            int time = this.getPullTime(stack) + (Integer)this.tolerance.get();
            if (this.mc.player.getItemUseTime() >= time) {
               this.mc.interactionManager.stopUsingItem(this.mc.player);
            }
      }
   }

   private void handleBow(Hand hand, double distance) {
      if (!this.mc.player.isUsingItem()) {
         this.mc.options.useKey.setPressed(true);
         this.mc.interactionManager.interactItem(this.mc.player, hand);
      } else {
         int useTicks = this.mc.player.getItemUseTime();
         int targetCharge = distance < 10.0 ? 12 : 20;
         if (useTicks >= targetCharge) {
            this.mc.interactionManager.stopUsingItem(this.mc.player);
            this.mc.options.useKey.setPressed(false);
            this.timer = (Integer)this.delay.get();
         }
      }
   }

   private void handleTrident(Hand hand) {
      if (!this.mc.player.isUsingItem()) {
         this.mc.options.useKey.setPressed(true);
         this.mc.interactionManager.interactItem(this.mc.player, hand);
      } else {
         int useTicks = this.mc.player.getItemUseTime();
         if (useTicks >= 12) {
            this.mc.interactionManager.stopUsingItem(this.mc.player);
            this.mc.options.useKey.setPressed(false);
            this.timer = (Integer)this.delay.get();
         }
      }
   }

   private float[] solveBallistic(Entity target, Item weapon, double dist) {
      double v = 1.0;
      double g = 0.05;
      if (weapon instanceof CrossbowItem) {
         v = 3.15;
         g = 0.05;
      } else if (weapon instanceof BowItem) {
         v = 3.0;
         g = 0.05;
      } else if (weapon == Items.TRIDENT) {
         v = 2.5;
         g = 0.05;
      } else if (weapon == Items.SNOWBALL || weapon == Items.EGG) {
         v = 1.5;
         g = 0.03;
      }

      Vec3d playerPos = this.mc.player.getEyePos();
      Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ()).add(0.0, target.getHeight() * 0.5, 0.0);
      Vec3d targetVel = new Vec3d(
         target.getX() - target.lastRenderX, target.getY() - target.lastRenderY, target.getZ() - target.lastRenderZ
      );
      Vec3d targetAccel = Vec3d.ZERO;
      if ((Boolean)this.calculateAcceleration.get() && this.prevVelocities.containsKey(target.getId())) {
         Vec3d lastVel = this.prevVelocities.get(target.getId());
         targetAccel = targetVel.subtract(lastVel);
         if (targetAccel.lengthSquared() > 1.0) {
            targetAccel = Vec3d.ZERO;
         }
      }

      if ((Boolean)this.predictMovement.get()) {
         double d = playerPos.distanceTo(targetPos);
         double t = d / v;
         double pingTicks = 0.0;
         if ((Boolean)this.pingCompensation.get() && this.mc.getNetworkHandler() != null && this.mc.player != null) {
            int latency = this.mc.getNetworkHandler().getPlayerListEntry(this.mc.player.getUuid()).getLatency();
            pingTicks = latency / 50.0;
         }

         double scale = (Double)this.predictionScale.get();

         for (int i = 0; i < 5; i++) {
            double totalTime = t + pingTicks;
            Vec3d velTerm = targetVel.multiply(totalTime);
            Vec3d accelTerm = targetAccel.multiply(0.5 * totalTime * totalTime);
            Vec3d prediction = velTerm;
            if ((Boolean)this.calculateAcceleration.get()) {
               prediction = velTerm.add(accelTerm);
            }

            Vec3d futurePos = targetPos.add(prediction.multiply(scale));
            double newDist = playerPos.distanceTo(futurePos);
            t = newDist / v;
         }

         double finalTime = t + pingTicks;
         Vec3d velTerm = targetVel.multiply(finalTime);
         Vec3d accelTerm = this.calculateAcceleration.get() ? targetAccel.multiply(0.5 * finalTime * finalTime) : Vec3d.ZERO;
         Vec3d prediction = velTerm.add(accelTerm).multiply(scale);
         targetPos = targetPos.add(prediction);
      }

      this.lastPredictedPos = targetPos;
      double dx = targetPos.x - playerPos.x;
      double dy = targetPos.y - playerPos.y;
      double dz = targetPos.z - playerPos.z;
      double distH = Math.sqrt(dx * dx + dz * dz);
      double v2 = v * v;
      double v4 = v2 * v2;
      double x2 = distH * distH;
      double root = v4 - g * (g * x2 + 2.0 * dy * v2);
      if (root < 0.0) {
         return null;
      } else {
         double angleRad = Math.atan2(v2 - Math.sqrt(root), g * distH);
         float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
         float pitch = (float)(-Math.toDegrees(angleRad));
         return new float[]{yaw, pitch};
      }
   }

   private int getPullTime(ItemStack stack) {
      try {
         Registry<Enchantment> registry = this.mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
         Reference<Enchantment> quickChargeEntry = registry.getOrThrow(Enchantments.QUICK_CHARGE);
         int level = EnchantmentHelper.getLevel(quickChargeEntry, stack);
         return Math.max(0, 25 - 5 * level);
      } catch (Exception var5) {
         return 25;
      }
   }

   private FindItemResult findWeapon() {
      return InvUtils.find(item -> this.isValidWeapon(item.getItem()));
   }

   private boolean isValidWeapon(Item item) {
      return item == Items.SNOWBALL || item == Items.EGG || item == Items.TRIDENT || item instanceof BowItem || item instanceof CrossbowItem;
   }

   private boolean isPet(Entity e) {
      return e instanceof TameableEntity tameable && tameable.isTamed() ? true : e instanceof AbstractHorseEntity horse && horse.isTame();
   }

   private GameMode getGameMode(PlayerEntity player) {
      if (this.mc.getNetworkHandler() == null) {
         return GameMode.SURVIVAL;
      } else {
         PlayerListEntry entry = this.mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
         return entry == null ? GameMode.SURVIVAL : entry.getGameMode();
      }
   }

   private Entity getEntityInCrosshair(double reachDistance) {
      Vec3d cameraPos = this.mc.player.getCameraPosVec(1.0F);
      Vec3d rotationVec = this.mc.player.getRotationVec(1.0F);
      Vec3d endPos = cameraPos.add(rotationVec.multiply(reachDistance));
      Box box = this.mc.player.getBoundingBox().stretch(rotationVec.multiply(reachDistance)).expand(1.0, 1.0, 1.0);
      EntityHitResult result = ProjectileUtil.raycast(
         this.mc.player, cameraPos, endPos, box, entity -> !entity.isSpectator() && entity.canHit(), reachDistance * reachDistance
      );
      return result != null ? result.getEntity() : null;
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.currentTarget != null) {
         boolean isLocked = this.priority.get() == CrossbowAura.PriorityMode.LockOnly && this.currentTarget == this.lockedTarget;
         SettingColor sColor = isLocked ? (SettingColor)this.lockSideColor.get() : (SettingColor)this.sideColor.get();
         SettingColor lColor = isLocked ? (SettingColor)this.lockLineColor.get() : (SettingColor)this.lineColor.get();
         event.renderer.box(this.currentTarget.getBoundingBox(), sColor, lColor, (ShapeMode)this.shapeMode.get(), 0);
         if ((Boolean)this.renderPredict.get() && this.lastPredictedPos != null) {
            double size = 0.3;
            Box pBox = new Box(
               this.lastPredictedPos.x - size,
               this.lastPredictedPos.y - size,
               this.lastPredictedPos.z - size,
               this.lastPredictedPos.x + size,
               this.lastPredictedPos.y + size,
               this.lastPredictedPos.z + size
            );
            event.renderer.box(pBox, new SettingColor(0, 255, 255, 80), new SettingColor(0, 255, 255, 200), ShapeMode.Both, 0);
         }
      }
   }

   public static enum AimMode {
      Silent,
      Lock;
   }

   public static enum CrossbowMode {
      Native,
      Control,
      Packet;
   }

   public static enum ListMode {
      Whitelist,
      Blacklist,
      Off;
   }

   public static enum PriorityMode {
      Closest("Distancemost"),
      LowestHealth("Healthmostlow"),
      LockOnly("AttackLock");

      private final String title;

      private PriorityMode(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }
}
