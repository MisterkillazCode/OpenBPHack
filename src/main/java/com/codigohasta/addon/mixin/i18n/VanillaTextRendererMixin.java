package com.codigohasta.addon.mixin.i18n;

import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {VanillaTextRenderer.class},
   remap = false
)
public class VanillaTextRendererMixin {
   @Shadow
   public boolean scaleIndividually;

   @Inject(
      method = {"end"},
      at = {@At("RETURN")}
   )
   public void endMixin(CallbackInfo ci) {
      this.scaleIndividually = true;
   }
}
