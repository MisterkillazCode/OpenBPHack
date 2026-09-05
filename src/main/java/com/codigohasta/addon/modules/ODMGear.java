package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class ODMGear extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgPhysics = this.settings.createGroup("Physics");
   private final SettingGroup sgInput = this.settings.createGroup("Controls");
   private final Setting<Double> range = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("range")).description("mostbigShoot")).defaultValue(64.0).min(10.0).build());
   private final Setting<Boolean> checkGear = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("require-gear"))
                  .description("OnlyatODM/Time"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> pullSpeed = this.sgPhysics
      .add(((Builder)((Builder)new Builder().name("pull-speed")).description("BasicPullPowerSpeed")).defaultValue(0.15).build());
   private final Setting<Double> orbitSpeed = this.sgPhysics
      .add(((Builder)((Builder)new Builder().name("orbit-speed")).description("A/D Key/Speed")).defaultValue(0.25).build());
   private final Setting<Double> lift = this.sgPhysics
      .add(((Builder)((Builder)new Builder().name("upward-lift")).description("up'soutsideRisePower(DefenseGround)")).defaultValue(0.08).build());
   private final Setting<Double> dualMultiplier = this.sgPhysics
      .add(((Builder)((Builder)new Builder().name("dual-multiplier")).description("HookTime'sSpeedRate")).defaultValue(1.5).build());
   private final Setting<Double> drag = this.sgPhysics
      .add(((Builder)((Builder)new Builder().name("drag")).description("AirAirPower(Hold)")).defaultValue(0.98).build());
   private final Setting<Keybind> leftKey = this.sgInput
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                  .name("left-hook"))
               .description("Left ClawKey"))
            .action(this::fireLeft)
            .build()
      );
   private final Setting<Keybind> rightKey = this.sgInput
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                  .name("right-hook"))
               .description("RightClawKey"))
            .action(this::fireRight)
            .build()
      );
   private final ODMGear.HookState leftHook = new ODMGear.HookState();
   private final ODMGear.HookState rightHook = new ODMGear.HookState();

   public ODMGear() {
      super(AddonTemplate.CATEGORY, "ODMGear", "Hook-based fly style; install-and-go movement. Feature module.");
   }

   public void onDeactivate() {
      this.leftHook.reset();
      this.rightHook.reset();
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if ((Boolean)this.checkGear.get()) {
            String chestId = this.mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().toString();
            if (!chestId.contains("odm_gear") && !chestId.contains("elytra")) {
               this.leftHook.reset();
               this.rightHook.reset();
               return;
            }
         }

         this.updateHookState(this.leftHook, (Keybind)this.leftKey.get());
         this.updateHookState(this.rightHook, (Keybind)this.rightKey.get());
         this.applyPhysics();
      }
   }

   private void updateHookState(ODMGear.HookState hook, Keybind key) {
      boolean isPressed = key.isPressed();
      if (isPressed && !hook.active) {
         this.fireHook(hook);
      } else if (!isPressed && hook.active) {
         hook.reset();
      }

      if (hook.active && hook.entity != null) {
         if (!hook.entity.isAlive()) {
            hook.reset();
         } else {
            Vec3d entityPos = new Vec3d(hook.entity.getX(), hook.entity.getY(), hook.entity.getZ());
            hook.pos = entityPos.add(0.0, hook.entity.getHeight() * 0.7, 0.0);
         }
      }
   }

   private void fireHook(ODMGear.HookState hook) {
      double dist = (Double)this.range.get();
      Vec3d eyes = this.mc.player.getEyePos();
      Vec3d look = this.mc.player.getRotationVec(1.0F);
      Vec3d end = eyes.add(look.multiply(dist));
      Box box = this.mc.player.getBoundingBox().stretch(look.multiply(dist)).expand(1.0);
      EntityHitResult entityHit = ProjectileUtil.raycast(
         this.mc.player, eyes, end, box, entity -> !entity.isSpectator() && entity.canHit(), dist * dist
      );
      BlockHitResult blockHit = this.mc
         .world
         .raycast(new RaycastContext(eyes, end, ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player));
      boolean hitEntity = entityHit != null;
      boolean hitBlock = blockHit.getType() != Type.MISS;
      if (hitEntity && hitBlock) {
         if (eyes.squaredDistanceTo(entityHit.getPos()) < eyes.squaredDistanceTo(blockHit.getPos())) {
            this.setHook(hook, entityHit.getEntity(), entityHit.getPos());
         } else {
            this.setHook(hook, null, blockHit.getPos());
         }
      } else if (hitEntity) {
         this.setHook(hook, entityHit.getEntity(), entityHit.getPos());
      } else if (hitBlock) {
         this.setHook(hook, null, blockHit.getPos());
      }
   }

   private void setHook(ODMGear.HookState hook, Entity entity, Vec3d pos) {
      hook.active = true;
      hook.entity = entity;
      hook.pos = pos;
   }

   private void applyPhysics() {
      if (this.leftHook.active || this.rightHook.active) {
         Vec3d playerPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
         boolean dualActive = this.leftHook.active && this.rightHook.active;
         Vec3d targetPos;
         if (dualActive) {
            targetPos = this.leftHook.pos.add(this.rightHook.pos).multiply(0.5);
         } else if (this.leftHook.active) {
            targetPos = this.leftHook.pos;
         } else {
            targetPos = this.rightHook.pos;
         }

         Vec3d toHook = targetPos.subtract(playerPos);
         Vec3d direction = toHook.normalize();
         boolean pressingForward = this.mc.player.input.playerInput.forward();
         boolean pressingA = this.mc.player.input.playerInput.left();
         boolean pressingD = this.mc.player.input.playerInput.right();
         boolean isOrbiting = (pressingA || pressingD) && !dualActive;
         double currentPullSpeed = (Double)this.pullSpeed.get();
         if (dualActive) {
            currentPullSpeed *= this.dualMultiplier.get();
         }

         if (pressingForward) {
            currentPullSpeed *= 1.2;
         }

         Vec3d pullForce = direction.multiply(currentPullSpeed);
         if (playerPos.y < targetPos.y) {
            pullForce = pullForce.add(0.0, (Double)this.lift.get(), 0.0);
         }

         Vec3d orbitForce = Vec3d.ZERO;
         if (isOrbiting) {
            Vec3d up = new Vec3d(0.0, 1.0, 0.0);
            Vec3d right = direction.crossProduct(up).normalize();
            if (right.lengthSquared() < 0.01) {
               right = direction.crossProduct(new Vec3d(1.0, 0.0, 0.0)).normalize();
            }

            double orbitMag = (Double)this.orbitSpeed.get();
            if (pressingD) {
               orbitForce = right.multiply(orbitMag);
            }

            if (pressingA) {
               orbitForce = right.multiply(-orbitMag);
            }
         }

         Vec3d currentVel = this.mc.player.getVelocity();
         Vec3d newVelocity = currentVel.multiply((Double)this.drag.get()).add(pullForce).add(orbitForce);
         this.mc.player.setVelocity(newVelocity);
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.leftHook.active) {
         this.renderLine(event, this.leftHook.pos);
      }

      if (this.rightHook.active) {
         this.renderLine(event, this.rightHook.pos);
      }
   }

   private void renderLine(Render3DEvent event, Vec3d target) {
      if (target != null) {
         Vec3d start = new Vec3d(this.mc.player.getX(), this.mc.player.getY() + 0.8, this.mc.player.getZ());
         event.renderer.line(start.x, start.y, start.z, target.x, target.y, target.z, Color.WHITE);
      }
   }

   private void fireLeft() {
   }

   private void fireRight() {
   }

   private static class HookState {
      boolean active = false;
      Vec3d pos = Vec3d.ZERO;
      Entity entity = null;

      void reset() {
         this.active = false;
         this.pos = Vec3d.ZERO;
         this.entity = null;
      }
   }
}
