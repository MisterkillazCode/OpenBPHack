package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.FireworkElytraFly;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardInput.class})
public abstract class MixinKeyboardInput extends Input {
   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onTickHead(CallbackInfo ci) {
      if (FireworkElytraFly.INSTANCE.isActive() && FireworkElytraFly.INSTANCE.clearInputTicks > 0) {
         boolean jump = FireworkElytraFly.INSTANCE.forceJumpInput;
         this.playerInput = new PlayerInput(false, false, false, false, jump, false, false);
         this.movementVector = new Vec2f(0.0F, 0.0F);
         ci.cancel();
      }
   }
}
