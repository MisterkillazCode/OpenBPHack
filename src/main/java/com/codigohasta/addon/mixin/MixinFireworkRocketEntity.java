package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.FireworkElytraFly;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({FireworkRocketEntity.class})
public abstract class MixinFireworkRocketEntity {
   @WrapOperation(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"
      )}
   )
   private Vec3d hookGetRotationVector(LivingEntity instance, Operation<Vec3d> original) {
      if (instance == MeteorClient.mc.player
         && FireworkElytraFly.INSTANCE.isActive()
         && FireworkElytraFly.INSTANCE.mode.get() == FireworkElytraFly.Mode.GrimDurability
         && (Boolean)FireworkElytraFly.INSTANCE.control.get()) {
         float yaw = FireworkElytraFly.INSTANCE.yaw;
         float pitch = FireworkElytraFly.INSTANCE.pitch;
         return instance.getRotationVector(pitch, yaw);
      } else {
         return (Vec3d)original.call(new Object[]{instance});
      }
   }
}
