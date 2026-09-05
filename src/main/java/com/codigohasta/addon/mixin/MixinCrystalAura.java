package com.codigohasta.addon.mixin;

import com.codigohasta.addon.utils.CrystalAuraMoveFixState;
import com.codigohasta.addon.utils.leaveshack.Rotation;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {CrystalAura.class},
   remap = false
)
public abstract class MixinCrystalAura {
   @Unique
   private Setting<CrystalAuraMoveFixState.Mode> img$movefixMode;
   @Unique
   private static double bphackAnimPitch = Double.NaN;
   @Unique
   private static final double BPITCH_STEP = 10.0;
   @Unique
   private static final double BPITCH_EPS = 1.5;
   @Shadow
   private boolean didRotateThisTick;
   @Shadow
   private boolean isLastRotationPos;
   @Shadow
   private Vec3d lastRotationPos;
   @Shadow
   private double lastYaw;
   @Shadow
   private double lastPitch;
   @Shadow
   private Setting<Boolean> rotate;
   @Shadow
   private int lastRotationTimer;
   @Shadow
   private Setting<Double> placeRange;
   @Shadow
   private Setting<Double> breakRange;

   @Shadow
   private int getLastRotationStopDelay() {
      return 0;
   }

   @Inject(
      method = {"isOutOfRange"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onIsOutOfRange(Vec3d vec3d, BlockPos blockPos, boolean place, CallbackInfoReturnable<Boolean> cir) {
      if (this.bphack$stuckInsideBlock()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null) {
            cir.setReturnValue(!PlayerUtils.isWithin(vec3d, (Double)(place ? this.placeRange : this.breakRange).get()));
         }
      }
   }

   @Unique
   private boolean bphack$stuckInsideBlock() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.world != null) {
         BlockPos eye = BlockPos.ofFloored(mc.player.getEyePos());
         BlockState state = mc.world.getBlockState(eye);
         return !state.isAir() && !state.getCollisionShape(mc.world, eye).isEmpty();
      } else {
         return false;
      }
   }

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void onInit(CallbackInfo ci) {
      Settings s = ((Module)this).settings;
      this.img$movefixMode = s.getDefaultGroup()
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("move-fix-mode"))
                     .description(
                        "How CrystalAura handles movement while it silently rotates. WalkOnly (default) leaves sprint/WASD untouched so you walk normally (no bounce). StopSprint forces sprint off while attacking (legacy, can bounce). None disables the feature."
                     ))
                  .defaultValue(CrystalAuraMoveFixState.Mode.WalkOnly))
               .build()
         );
   }

   @Inject(
      method = {"onPreTick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onPreTickHead(Pre event, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) {
         ci.cancel();
         CrystalAuraMoveFixState.active = false;
         CrystalAuraMoveFixState.attacking = false;
         Rotation.rotation = false;
      } else {
         CrystalAuraMoveFixState.mode = (CrystalAuraMoveFixState.Mode)this.img$movefixMode.get();
         CrystalAuraMoveFixState.active = CrystalAuraMoveFixState.mode != CrystalAuraMoveFixState.Mode.None;
         if (!CrystalAuraMoveFixState.active) {
            Rotation.rotation = false;
         }
      }
   }

   @Inject(
      method = {"onPreTickLast"},
      at = {@At("HEAD")}
   )
   private void onPreTickLastHead(Pre event, CallbackInfo ci) {
      if (CrystalAuraMoveFixState.active && (Boolean)this.rotate.get()) {
         boolean rotatingThisTick = this.didRotateThisTick || this.lastRotationTimer < this.getLastRotationStopDelay();
         if (rotatingThisTick) {
            float yaw = this.isLastRotationPos ? (float)Rotations.getYaw(this.lastRotationPos) : (float)this.lastYaw;
            float pitch = this.isLastRotationPos ? (float)Rotations.getPitch(this.lastRotationPos) : (float)this.lastPitch;
            Rotation.snapAt(yaw, pitch);
            CrystalAuraMoveFixState.attacking = true;
         } else {
            Rotation.rotation = false;
            CrystalAuraMoveFixState.attacking = false;
         }
      } else {
         Rotation.rotation = false;
         CrystalAuraMoveFixState.attacking = false;
      }
   }

   @Inject(
      method = {"onDeactivate"},
      at = {@At("HEAD")}
   )
   private void onDeactivateHead(CallbackInfo ci) {
      CrystalAuraMoveFixState.active = false;
      CrystalAuraMoveFixState.attacking = false;
      Rotation.rotation = false;
      bphackAnimPitch = Double.NaN;
   }

   @Redirect(
      method = {"onPreTickLast", "doBreak(Lnet/minecraft/entity/Entity;)V", "doPlace", "doYawSteps"},
      at = @At(
         value = "INVOKE",
         target = "Lmeteordevelopment/meteorclient/utils/player/Rotations;rotate(DDILjava/lang/Runnable;)V"
      )
   )
   private static void bphackSmoothRotate(double yaw, double pitch, int priority, Runnable callback) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) {
         Rotations.rotate(yaw, pitch, priority, callback);
      } else {
         if (Double.isNaN(bphackAnimPitch)) {
            bphackAnimPitch = mc.player.getPitch();
         }

         double diff = pitch - bphackAnimPitch;
         double abs = Math.abs(diff);
         if (abs <= 1.5) {
            bphackAnimPitch = pitch;
         } else {
            double step = Math.min(10.0, Math.max(abs * 0.35, 1.5));
            bphackAnimPitch = bphackAnimPitch + Math.signum(diff) * step;
         }

         boolean aligned = Math.abs(pitch - bphackAnimPitch) <= 1.5;
         Rotations.rotate(yaw, bphackAnimPitch, priority, aligned ? callback : null);
      }
   }
}
