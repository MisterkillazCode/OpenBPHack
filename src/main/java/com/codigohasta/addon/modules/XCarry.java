package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

public class XCarry extends Module {
   public XCarry() {
      super(
         AddonTemplate.SC_CATEGORY,
         "XCarry",
         "Lets you place items into the crafting/ender inventory; items won't drop when the screen closes (syncId=0 pack)."
      );
   }

   @EventHandler
   private void onPacketSend(Send event) {
      if (event.packet instanceof CloseHandledScreenC2SPacket packet && packet.getSyncId() == 0) {
         event.cancel();
      }
   }
}
