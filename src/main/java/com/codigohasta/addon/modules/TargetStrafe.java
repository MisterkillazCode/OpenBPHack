package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class TargetStrafe extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<TargetStrafe.MoveMode> mode = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("MoveMode")).description("Move'sMethodMode.")).defaultValue(TargetStrafe.MoveMode.Basic)).build());
   public final Setting<TargetStrafe.ExecuteMode> executeMode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("RowMode")).description("atTimeRowMove. 'MoveTime'Modemore."))
               .defaultValue(TargetStrafe.ExecuteMode.WhileMoving))
            .build()
      );
   public final Setting<Double> targetRange = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("TargetRange"))
               .description("Target'smostbigRange."))
            .defaultValue(7.0)
            .sliderRange(1.0, 15.0)
            .build()
      );
   public final Setting<Boolean> targetCreative = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AttackBuild"))
                  .description("whetherBuildMode'sPlayerViewForTarget."))
               .defaultValue(false))
            .build()
      );
   public final Setting<Boolean> targetAdventure = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Attack"))
                  .description("whetherMode'sPlayerViewForTarget."))
               .defaultValue(false))
            .build()
      );
   public final Setting<Double> radius = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("Circle Radius"))
               .description("andTargetHold'sDistance."))
            .defaultValue(2.5)
            .min(0.1)
            .sliderRange(0.1, 10.0)
            .build()
      );
   public final Setting<Double> speed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("Speed"))
               .description("TargetMove'sBasicSpeed."))
            .defaultValue(0.24)
            .min(0.0)
            .sliderRange(0.0, 2.0)
            .build()
      );
   public final Setting<Double> scrollSpeed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("ScrollMoveSpeed"))
                  .description("at'ScrollMove'Modedown'soutsideSpeed."))
               .defaultValue(0.26)
               .min(0.0)
               .sliderRange(0.0, 2.0)
               .visible(() -> this.mode.get() == TargetStrafe.MoveMode.Scroll))
            .build()
      );
   public final Setting<Boolean> damageBoost = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Damage"))
                  .description("youTake DamageTimeTimeIncreaseMove Speed."))
               .defaultValue(true))
            .build()
      );
   public final Setting<Double> boost = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("Boost Value"))
                  .description("Take DamageTimeoutside'sSpeed."))
               .defaultValue(0.1)
               .min(0.0)
               .sliderRange(0.0, 0.5)
               .visible(this.damageBoost::get))
            .build()
      );
   private PlayerEntity target;
   private int direction = 1;

   public TargetStrafe() {
      super(
         AddonTemplate.CATEGORY,
         "TargetStrafe",
         "Circles around your target while attacking, with configurable radius, speed and jump boost. (Unreliable - Meteor-style v2 port.)"
      );
   }

   @EventHandler
   private void onTick(Pre event) {
      this.target = (PlayerEntity)TargetUtils.get(this::isValidTarget, SortPriority.ClosestAngle);
      if (this.target != null) {
         if (this.mc.player.isOnGround()) {
            this.mc.player.jump();
         }

         if (this.mc.options.leftKey.isPressed()) {
            this.direction = 1;
         } else if (this.mc.options.rightKey.isPressed()) {
            this.direction = -1;
         }

         if (this.mc.player.horizontalCollision) {
            this.direction *= -1;
         }

         if (this.executeMode.get() == TargetStrafe.ExecuteMode.GameTick) {
            double currentSpeed = (Double)this.speed.get() + (this.damageBoost.get() && this.mc.player.hurtTime > 0 ? (Double)this.boost.get() : 0.0);
            double forward = this.mc.player.distanceTo(this.target) > this.radius.get() ? 1.0 : 0.0;
            float yaw = (float)Rotations.getYaw(this.target);
            this.mc.player.bodyYaw = yaw;
            this.mc.player.headYaw = yaw;
            this.mc.player.setVelocity(this.applySpeed(yaw, currentSpeed, forward, this.direction));
         }
      }
   }

   @EventHandler
   private void onMove(PlayerMoveEvent event) {
      if (this.executeMode.get() == TargetStrafe.ExecuteMode.WhileMoving && this.target != null && this.isValidTarget(this.target)) {
         double currentSpeed = (Double)this.speed.get() + (this.damageBoost.get() && this.mc.player.hurtTime > 0 ? (Double)this.boost.get() : 0.0);
         double forward = this.mc.player.distanceTo(this.target) > this.radius.get() ? 1.0 : 0.0;
         float yaw = (float)Rotations.getYaw(this.target);
         Vec3d newVelocity = this.applySpeed(yaw, currentSpeed, forward, this.direction);
         event.movement = new Vec3d(newVelocity.x, event.movement.y, newVelocity.z);
      }
   }

   private boolean isValidTarget(Entity entity) {
      if (entity instanceof PlayerEntity player) {
         if (player == this.mc.player) {
            return false;
         } else if (!player.isDead() && player.isAlive()) {
            if (player.distanceTo(this.mc.player) > (Double)this.targetRange.get()) {
               return false;
            } else if (Friends.get().isFriend(player)) {
               return false;
            } else {
               GameMode gm = this.getGameMode(player);
               if (gm == GameMode.SPECTATOR) {
                  return false;
               } else {
                  return gm == GameMode.CREATIVE && !this.targetCreative.get() ? false : gm != GameMode.ADVENTURE || (Boolean)this.targetAdventure.get();
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private GameMode getGameMode(PlayerEntity player) {
      if (player == null) {
         return null;
      } else if (this.mc.getNetworkHandler() == null) {
         return null;
      } else {
         PlayerListEntry entry = this.mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
         return entry == null ? null : entry.getGameMode();
      }
   }

   private Vec3d applySpeed(float yaw, double speed, double forward, double direction) {
      if (forward != 0.0) {
         if (direction > 0.0) {
            yaw += forward > 0.0 ? -45 : 45;
         } else if (direction < 0.0) {
            yaw += forward > 0.0 ? 45 : -45;
         }

         direction = 0.0;
         if (forward > 0.0) {
            forward = 1.0;
         } else if (forward < 0.0) {
            forward = -1.0;
         }
      }

      double cos = Math.cos(Math.toRadians(yaw + 90.0F));
      double sin = Math.sin(Math.toRadians(yaw + 90.0F));
      return new Vec3d(
         forward * speed * cos + direction * speed * sin, this.mc.player.getVelocity().y, forward * speed * sin - direction * speed * cos
      );
   }

   public static enum ExecuteMode {
      GameTick,
      WhileMoving;
   }

   public static enum MoveMode {
      Basic,
      Scroll;
   }
}
