package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ElytraFlyPlus extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgSpeed = this.settings.createGroup("SpeedSetting");
   private final Setting<ElytraFlyPlus.Mode> mode = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Mode")).description("Fly'sSystemMode.")).defaultValue(ElytraFlyPlus.Mode.Wasp)).build());
   private final Setting<Boolean> stopWater = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("WaterinStopStop"))
                  .description("atWaterinTimenotMove."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> stopLava = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("LavainStopStop"))
                  .description("atLavainTimenotMove."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> horizontal = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("WaterSpeed"))
                  .description("TickWaterMove'sBlockNumber."))
               .defaultValue(1.0)
               .min(0.0)
               .sliderRange(0.0, 5.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Wasp))
            .build()
      );
   private final Setting<Double> up = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("RiseSpeed"))
                  .description("TickRise'sBlockNumber."))
               .defaultValue(1.0)
               .min(0.0)
               .sliderRange(0.0, 5.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Wasp))
            .build()
      );
   private final Setting<Double> speed = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("BasicSpeed"))
                  .description("TickMove'sBasicSpeed."))
               .defaultValue(1.0)
               .min(0.0)
               .sliderRange(0.0, 5.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Control))
            .build()
      );
   private final Setting<Double> upMultiplier = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("RiseRate"))
                  .description("upFlyTime'sSpeedRate."))
               .defaultValue(1.0)
               .min(0.0)
               .sliderRange(0.0, 5.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Control))
            .build()
      );
   private final Setting<Double> down = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("downSpeed"))
                  .description("Tickdown'sBlockNumber."))
               .defaultValue(1.0)
               .min(0.0)
               .sliderRange(0.0, 5.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Control || this.mode.get() == ElytraFlyPlus.Mode.Wasp))
            .build()
      );
   private final Setting<Boolean> smartFall = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("candown"))
                     .description("atlookdownDirectionTimefastdown."))
                  .defaultValue(true))
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Wasp))
            .build()
      );
   private final Setting<Double> fallSpeed = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("Speed"))
                  .description("TickSelfdown'sSpeed."))
               .defaultValue(0.01)
               .min(0.0)
               .sliderRange(0.0, 1.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Control || this.mode.get() == ElytraFlyPlus.Mode.Wasp))
            .build()
      );
   private final Setting<Double> constSpeed = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("SetSpeed"))
                  .description("Constantiam Mode'smostbigSpeed."))
               .defaultValue(1.0)
               .min(0.0)
               .sliderRange(0.0, 5.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Constantiam))
            .build()
      );
   private final Setting<Double> constAcceleration = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("SetSpeed"))
                  .description("Constantiam Mode'sSpeed."))
               .defaultValue(1.0)
               .min(0.0)
               .sliderRange(0.0, 5.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Constantiam))
            .build()
      );
   private final Setting<Boolean> constStop = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("noStopStop"))
                     .description("noKeyTimeStopStopMove."))
                  .defaultValue(true))
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Constantiam))
            .build()
      );
   private boolean moving;
   private float yaw;
   private float pitch;
   private float p;
   private double velocity;
   private int activeFor;

   public ElytraFlyPlus() {
      super(AddonTemplate.CATEGORY, "ElytraFlyPlus", "Blackout variant of elytra fly.");
   }

   @EventHandler(
      priority = 200
   )
   private void onMove(PlayerMoveEvent event) {
      if (this.shouldRun()) {
         this.activeFor++;
         if (this.activeFor >= 5) {
            switch ((ElytraFlyPlus.Mode)this.mode.get()) {
               case Wasp:
                  this.waspTick(event);
                  break;
               case Control:
                  this.controlTick(event);
                  break;
               case Constantiam:
                  this.constantiamTick(event);
            }
         }
      }
   }

   private void constantiamTick(PlayerMoveEvent event) {
      Vec3d motion = this.getMotion(this.mc.player.getVelocity());
      if (motion != null) {
         ((IVec3d)event.movement).meteor$set(motion.getX(), motion.getY(), motion.getZ());
         event.movement = motion;
      }
   }

   private Vec3d getMotion(Vec3d velocity) {
      float forwardInput = (this.mc.player.input.playerInput.forward() ? 1.0F : 0.0F)
         - (this.mc.player.input.playerInput.backward() ? 1.0F : 0.0F);
      if (forwardInput == 0.0F) {
         return this.constStop.get() ? new Vec3d(0.0, 0.0, 0.0) : null;
      } else {
         boolean forward = forwardInput > 0.0F;
         double yaw = Math.toRadians(this.mc.player.getYaw() + (forward ? 90 : -90));
         double x = Math.cos(yaw);
         double z = Math.sin(yaw);
         double maxAcc = this.calcAcceleration(velocity.x, velocity.z, x, z);
         double progress = (velocity.horizontalLength() - 0.0) / 0.5;
         double delta = MathHelper.clamp(progress, 0.0, 1.0);
         double acc = Math.min(maxAcc, (Double)this.constAcceleration.get() / 20.0 * (0.1 + delta * 0.9));
         return new Vec3d(velocity.getX() + x * acc, velocity.getY(), velocity.getZ() + z * acc);
      }
   }

   private double calcAcceleration(double vx, double vz, double x, double z) {
      double xz = x * x + z * z;
      return (
            Math.sqrt(xz * (Double)this.constSpeed.get() * (Double)this.constSpeed.get() - x * x * vz * vz - z * z * vx * vx + 2.0 * x * z * vx * vz)
               - x * vx
               - z * vz
         )
         / xz;
   }

   private void waspTick(PlayerMoveEvent event) {
      if (this.mc.player.isGliding()) {
         this.updateWaspMovement();
         this.pitch = this.mc.player.getPitch();
         double cos = Math.cos(Math.toRadians(this.yaw + 90.0F));
         double sin = Math.sin(Math.toRadians(this.yaw + 90.0F));
         double x = this.moving ? cos * (Double)this.horizontal.get() : 0.0;
         double y = -(Double)this.fallSpeed.get();
         double z = this.moving ? sin * (Double)this.horizontal.get() : 0.0;
         if ((Boolean)this.smartFall.get()) {
            y *= Math.abs(Math.sin(Math.toRadians(this.pitch)));
         }

         if (this.mc.options.sneakKey.isPressed() && !this.mc.options.jumpKey.isPressed()) {
            y = -(Double)this.down.get();
         }

         if (!this.mc.options.sneakKey.isPressed() && this.mc.options.jumpKey.isPressed()) {
            y = (Double)this.up.get();
         }

         ((IVec3d)event.movement).meteor$set(x, y, z);
         this.mc.player.setVelocity(0.0, 0.0, 0.0);
      }
   }

   private void updateWaspMovement() {
      float yaw = this.mc.player.getYaw();
      float f = (this.mc.player.input.playerInput.forward() ? 1.0F : 0.0F) - (this.mc.player.input.playerInput.backward() ? 1.0F : 0.0F);
      float s = (this.mc.player.input.playerInput.left() ? 1.0F : 0.0F) - (this.mc.player.input.playerInput.right() ? 1.0F : 0.0F);
      if (f > 0.0F) {
         this.moving = true;
         yaw += s > 0.0F ? -45.0F : (s < 0.0F ? 45.0F : 0.0F);
      } else if (f < 0.0F) {
         this.moving = true;
         yaw += s > 0.0F ? -135.0F : (s < 0.0F ? 135.0F : 180.0F);
      } else {
         this.moving = s != 0.0F;
         yaw += s > 0.0F ? -90.0F : (s < 0.0F ? 90.0F : 0.0F);
      }

      this.yaw = yaw;
   }

   private void controlTick(PlayerMoveEvent event) {
      if (this.mc.player.isGliding()) {
         this.updateControlMovement();
         this.pitch = 0.0F;
         boolean movingUp = false;
         if (!this.mc.options.sneakKey.isPressed() && this.mc.options.jumpKey.isPressed() && this.velocity > (Double)this.speed.get() * 0.4) {
            this.p = (float)Math.min(this.p + 0.1 * (1.0F - this.p) * (1.0F - this.p) * (1.0F - this.p), 1.0);
            this.pitch = Math.max(Math.max(this.p, 0.0F) * -90.0F, -90.0F);
            movingUp = true;
            this.moving = false;
         } else {
            this.velocity = (Double)this.speed.get();
            this.p = -0.2F;
         }

         this.velocity = this.moving
            ? (Double)this.speed.get()
            : Math.min(this.velocity + Math.sin(Math.toRadians(this.pitch)) * 0.08, (Double)this.speed.get());
         double cos = Math.cos(Math.toRadians(this.yaw + 90.0F));
         double sin = Math.sin(Math.toRadians(this.yaw + 90.0F));
         double x = this.moving && !movingUp ? cos * (Double)this.speed.get() : (movingUp ? this.velocity * Math.cos(Math.toRadians(this.pitch)) * cos : 0.0);
         double y = this.pitch < 0.0F
            ? this.velocity * (Double)this.upMultiplier.get() * -Math.sin(Math.toRadians(this.pitch)) * this.velocity
            : -(Double)this.fallSpeed.get();
         double z = this.moving && !movingUp ? sin * (Double)this.speed.get() : (movingUp ? this.velocity * Math.cos(Math.toRadians(this.pitch)) * sin : 0.0);
         y *= Math.abs(Math.sin(Math.toRadians(movingUp ? this.pitch : this.mc.player.getPitch())));
         if (this.mc.options.sneakKey.isPressed() && !this.mc.options.jumpKey.isPressed()) {
            y = -(Double)this.down.get();
         }

         ((IVec3d)event.movement).meteor$set(x, y, z);
         this.mc.player.setVelocity(0.0, 0.0, 0.0);
      }
   }

   private void updateControlMovement() {
      float yaw = this.mc.player.getYaw();
      float f = (this.mc.player.input.playerInput.forward() ? 1.0F : 0.0F) - (this.mc.player.input.playerInput.backward() ? 1.0F : 0.0F);
      float s = (this.mc.player.input.playerInput.left() ? 1.0F : 0.0F) - (this.mc.player.input.playerInput.right() ? 1.0F : 0.0F);
      if (f > 0.0F) {
         this.moving = true;
         yaw += s > 0.0F ? -45.0F : (s < 0.0F ? 45.0F : 0.0F);
      } else if (f < 0.0F) {
         this.moving = true;
         yaw += s > 0.0F ? -135.0F : (s < 0.0F ? 135.0F : 180.0F);
      } else {
         this.moving = s != 0.0F;
         yaw += s > 0.0F ? -90.0F : (s < 0.0F ? 90.0F : 0.0F);
      }

      this.yaw = yaw;
   }

   public boolean shouldRun() {
      if ((Boolean)this.stopWater.get() && this.mc.player.isTouchingWater()) {
         this.activeFor = 0;
         return false;
      } else if ((Boolean)this.stopLava.get() && this.mc.player.isInLava()) {
         this.activeFor = 0;
         return false;
      } else {
         return this.mc.player.isGliding();
      }
   }

   public static enum Mode {
      Wasp,
      Control,
      Constantiam;
   }
}
