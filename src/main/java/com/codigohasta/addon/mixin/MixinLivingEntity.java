package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.FireworkElytraFly;
import com.codigohasta.addon.modules.GlobalSetting;
import com.codigohasta.addon.utils.leaveshack.Rotation;
import com.codigohasta.addon.utils.leaveshack.events.ElytraUpdateEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LivingEntity.class})
public class MixinLivingEntity {
   @WrapOperation(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;isGliding()Z"
      )}
   )
   private boolean wrapIsGliding(LivingEntity instance, Operation<Boolean> original) {
      if (instance == MeteorClient.mc.player) {
         ElytraUpdateEvent elytraTransformEvent = new ElytraUpdateEvent(instance);
         MeteorClient.EVENT_BUS.post(elytraTransformEvent);
         FireworkElytraFly.INSTANCE.isFallFlying = (Boolean)original.call(new Object[]{instance});
         if (elytraTransformEvent.isCancelled()) {
            return false;
         }
      }

      return (Boolean)original.call(new Object[]{instance});
   }

   @WrapOperation(
      method = {"jump"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"
      )}
   )
   private float wrapGetYaw(LivingEntity instance, Operation<Float> original) {
      return GlobalSetting.INSTANCE.moveFix.get() && Rotation.rotation ? Rotation.targetYaw : (Float)original.call(new Object[]{instance});
   }
}
