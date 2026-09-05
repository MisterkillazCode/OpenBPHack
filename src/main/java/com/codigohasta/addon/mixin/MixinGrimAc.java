package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.GrimAc;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayNetworkHandler.class})
public abstract class MixinGrimAc {
   @Inject(
      method = {"sendChatCommand(Ljava/lang/String;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSendChatCommand(String command, CallbackInfo ci) {
      if (command != null && command.trim().equalsIgnoreCase("GrimAc")) {
         GrimAc.toggleFromCommand();
         ci.cancel();
      }
   }

   @Inject(
      method = {"sendChatMessage(Ljava/lang/String;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSendChatMessage(String message, CallbackInfo ci) {
      if (message != null) {
         String m = message.trim();
         if (m.equalsIgnoreCase("/GrimAc") || m.equalsIgnoreCase("GrimAc")) {
            GrimAc.toggleFromCommand();
            ci.cancel();
         }
      }
   }
}
