package com.codigohasta.addon.mixin;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {ClientBrandRetriever.class},
   remap = false
)
public class MixinClientBrandRetriever {
   @Inject(
      method = {"getClientModName"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void onGetClientModName(CallbackInfoReturnable<String> cir) {
      cir.setReturnValue("vanilla");
   }
}
