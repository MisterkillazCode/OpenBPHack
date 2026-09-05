package com.codigohasta.addon.mixin;

import com.codigohasta.addon.utils.alien.AlienRotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public class MixinLivingEntitySprint {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   @Inject(
      method = {"jump"},
      at = {@At("HEAD")}
   )
   private void onJumpPre(CallbackInfo ci) {
      if (mc != null && mc.player != null && this == mc.player) {
         if (AlienRotationUtil.shouldRotate) {
            AlienRotationUtil.preYaw = mc.player.getYaw();
            mc.player.setYaw(AlienRotationUtil.sprintYaw);
         }
      }
   }

   @Inject(
      method = {"jump"},
      at = {@At("RETURN")}
   )
   private void onJumpPost(CallbackInfo ci) {
      if (mc != null && mc.player != null && this == mc.player) {
         if (AlienRotationUtil.shouldRotate) {
            mc.player.setYaw(AlienRotationUtil.preYaw);
         }
      }
   }
}
