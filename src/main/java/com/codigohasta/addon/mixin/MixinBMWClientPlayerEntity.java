package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.BMWSprint;
import com.codigohasta.addon.utils.bmw.BMWPlayerUtil;
import com.codigohasta.addon.utils.bmw.BMWRotationUtil;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayerEntity.class})
public class MixinBMWClientPlayerEntity {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   @Inject(
      method = {"sendMovementPackets"},
      at = {@At("HEAD")}
   )
   private void onSendMovementPacketsHead(CallbackInfo ci) {
      if (BMWRotationUtil.shouldRotate) {
         ClientPlayerEntity player = (ClientPlayerEntity)this;
         BMWRotationUtil.preYaw = player.getYaw();
         BMWRotationUtil.preBodyYaw = player.bodyYaw;
         BMWRotationUtil.preHeadYaw = player.headYaw;
         player.setYaw(BMWRotationUtil.sprintYaw);
         player.bodyYaw = BMWRotationUtil.sprintYaw;
         player.headYaw = BMWRotationUtil.sprintYaw;
      }
   }

   @Inject(
      method = {"sendMovementPackets"},
      at = {@At("TAIL")}
   )
   private void onSendMovementPacketsTail(CallbackInfo ci) {
      if (BMWRotationUtil.shouldRotate) {
         ClientPlayerEntity player = (ClientPlayerEntity)this;
         player.setYaw(BMWRotationUtil.preYaw);
      }
   }

   @WrapWithCondition(
      method = {"tickMovement"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;setSprinting(Z)V",
         ordinal = 3
      )}
   )
   private boolean wrapStopSprinting(ClientPlayerEntity instance, boolean b) {
      return BMWSprint.INSTANCE == null
         || !BMWSprint.INSTANCE.isActive()
         || BMWSprint.INSTANCE.sprintMode.get() != BMWSprint.Mode.OMNIROTATIONAL && BMWSprint.INSTANCE.sprintMode.get() != BMWSprint.Mode.OMNIDIRECTIONAL
         || !BMWPlayerUtil.isMoving();
   }
}
