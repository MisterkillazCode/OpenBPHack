package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.ServerFix;
import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.ClientConnection;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientConnection.class})
public class MixinClientConnectionServerFix {
   @Unique
   private static final Logger LOGGER = LogUtils.getLogger();
   @Unique
   private static final String[] LEGACY_SKIP_PACKETS = new String[]{"update_advancements", "bundle_delimiter", "add_entity"};

   @Inject(
      method = {"exceptionCaught"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onExceptionCaught(ChannelHandlerContext ctx, Throwable throwable, CallbackInfo ci) {
      if (this.isSkippableLegacyPacketError(throwable)) {
         ci.cancel();
      }
   }

   @Unique
   private boolean isSkippableLegacyPacketError(Throwable throwable) {
      if (ServerFix.INSTANCE == null || !ServerFix.INSTANCE.isActive()) {
         return false;
      } else if (!(throwable instanceof DecoderException)) {
         return false;
      } else {
         Throwable t = throwable;

         for (int i = 0; t != null && i < 10; i++) {
            String msg = t.getMessage();
            if (msg != null) {
               String m = msg.toLowerCase();

               for (String pkt : LEGACY_SKIP_PACKETS) {
                  if (m.contains(pkt)) {
                     LOGGER.warn("[ServerFix] swallowed undecodable legacy packet: {}", msg);
                     return true;
                  }
               }

               if (m.contains("packet") || m.contains("decode")) {
                  LOGGER.warn("[ServerFix] NOT swallowing decoder error (will disconnect): {}", msg);
               }
            }

            t = t.getCause();
         }

         return false;
      }
   }
}
