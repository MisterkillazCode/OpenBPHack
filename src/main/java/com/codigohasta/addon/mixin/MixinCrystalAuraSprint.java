package com.codigohasta.addon.mixin;

import com.codigohasta.addon.utils.CrystalAuraMoveFixState;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayerEntity.class})
public class MixinCrystalAuraSprint {
   @Inject(
      method = {"tickMovement"},
      at = {@At("HEAD")}
   )
   private void forceStopSprintBeforeTravel(CallbackInfo ci) {
      if (this.shouldStopSprint()) {
         ClientPlayerEntity player = (ClientPlayerEntity)this;
         if (player.isSprinting()) {
            player.setSprinting(false);
         }
      }
   }

   @Inject(
      method = {"sendSprintingPacket"},
      at = {@At("HEAD")}
   )
   private void forceStopSprintBeforePacket(CallbackInfo ci) {
      if (this.shouldStopSprint()) {
         ClientPlayerEntity player = (ClientPlayerEntity)this;
         if (player.isSprinting()) {
            player.setSprinting(false);
         }
      }
   }

   @Unique
   private boolean shouldStopSprint() {
      if (Modules.get() == null) {
         return false;
      } else {
         CrystalAura ca = (CrystalAura)Modules.get().get(CrystalAura.class);
         return ca != null && ca.isActive() ? CrystalAuraMoveFixState.mode == CrystalAuraMoveFixState.Mode.StopSprint && CrystalAuraMoveFixState.active : false;
      }
   }
}
