package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.AdvancementUpdateS2CPacket;

public class ServerFix extends Module {
   public static ServerFix INSTANCE;

   public ServerFix() {
      super(
         AddonTemplate.CATEGORY,
         "ServerFix",
         "Fixes joining 1.12.2 (or other legacy) servers by silently swallowing DecoderException for malformed packet payloads (e.g. update_advancements, bundle_delimiter) that would otherwise boot you to the title screen with 'Failed to decode packet'."
      );
      INSTANCE = this;
   }

   @EventHandler
   public void onPacketReceive(Receive event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (event.packet instanceof AdvancementUpdateS2CPacket) {
            event.cancel();
         }
      }
   }
}
