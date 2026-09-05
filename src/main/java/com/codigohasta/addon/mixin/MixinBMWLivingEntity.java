package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.BMWSprint;
import com.codigohasta.addon.utils.bmw.BMWDirectionalInput;
import com.codigohasta.addon.utils.bmw.BMWPlayerUtil;
import com.codigohasta.addon.utils.bmw.BMWRotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {LivingEntity.class},
   priority = 1500
)
public class MixinBMWLivingEntity {
   @Unique
   private static float invincible$sprintYaw = Float.NaN;
   @Unique
   private static Vec3d invincible$preJumpVelocity = null;

   @Inject(
      method = {"jump"},
      at = {@At("HEAD")}
   )
   private void onJumpPre(CallbackInfo ci) {
      invincible$sprintYaw = Float.NaN;
      invincible$preJumpVelocity = null;
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null && mc.player != null && this == mc.player) {
         if (BMWSprint.INSTANCE != null && BMWSprint.INSTANCE.isActive()) {
            BMWSprint.Mode mode = (BMWSprint.Mode)BMWSprint.INSTANCE.sprintMode.get();
            if (mode == BMWSprint.Mode.OMNIDIRECTIONAL || mode == BMWSprint.Mode.OMNIROTATIONAL) {
               mc.player.setSprinting(true);
               invincible$preJumpVelocity = mc.player.getVelocity();
               if (mode == BMWSprint.Mode.OMNIDIRECTIONAL) {
                  invincible$sprintYaw = BMWPlayerUtil.getMovementDirectionOfInput(mc.player.getYaw(), BMWDirectionalInput.fromPlayer());
               } else if (mode == BMWSprint.Mode.OMNIROTATIONAL && BMWRotationManager.targetRotation != null) {
                  invincible$sprintYaw = BMWRotationManager.targetRotation.yaw;
               }
            }
         }
      }
   }

   @Inject(
      method = {"jump"},
      at = {@At("RETURN")}
   )
   private void onJumpPost(CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null || mc.player == null || this != mc.player) {
         invincible$sprintYaw = Float.NaN;
         invincible$preJumpVelocity = null;
      } else if (invincible$preJumpVelocity != null && !Float.isNaN(invincible$sprintYaw)) {
         Vec3d currentVel = mc.player.getVelocity();
         double dx = currentVel.x - invincible$preJumpVelocity.x;
         double dz = currentVel.z - invincible$preJumpVelocity.z;
         if (Math.abs(dx) > 1.0E-6 || Math.abs(dz) > 1.0E-6) {
            float yawRad = invincible$sprintYaw * (float) (Math.PI / 180.0);
            double correctX = -MathHelper.sin(yawRad) * 0.2;
            double correctZ = MathHelper.cos(yawRad) * 0.2;
            mc.player.setVelocity(currentVel.x - dx + correctX, currentVel.y, currentVel.z - dz + correctZ);
         }

         invincible$sprintYaw = Float.NaN;
         invincible$preJumpVelocity = null;
      } else {
         invincible$sprintYaw = Float.NaN;
         invincible$preJumpVelocity = null;
      }
   }
}
