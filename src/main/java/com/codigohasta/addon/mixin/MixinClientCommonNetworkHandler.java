package com.codigohasta.addon.mixin;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientCommonNetworkHandler.class})
public class MixinClientCommonNetworkHandler {
   @Inject(
      method = {"sendPacket"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
      if (packet instanceof CustomPayloadC2SPacket customPayloadPacket) {
         try {
            String channelId = customPayloadPacket.payload().getId().id().toString().toLowerCase();
            if (channelId.contains("modmenu") || channelId.contains("meteor") || channelId.contains("baritone") || channelId.contains("fabric")) {
               ci.cancel();
            }
         } catch (Exception var5) {
         }
      }
   }
}
