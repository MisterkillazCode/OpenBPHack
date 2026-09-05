package com.codigohasta.addon.mixin;

import com.codigohasta.addon.events.MovementInputEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayerEntity.class})
public abstract class ClientPlayerEntityMixin {
   @Shadow
   public Input input;

   @Inject(
      method = {"tickMovement"},
      at = {@At("TAIL")}
   )
   private void onAfterTickMovement(CallbackInfo ci) {
      if (this.input != null) {
         MeteorClient.EVENT_BUS.post(MovementInputEvent.get(this.input));
      }
   }
}
