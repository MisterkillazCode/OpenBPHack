package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;

public class PortalGodMode extends Module {
   private final List<TeleportConfirmC2SPacket> packets = new ArrayList<>();

   public PortalGodMode() {
      super(AddonTemplate.SC_CATEGORY, "PortalGodMode", "Grants temporary invulnerability after entering a portal. You cannot move, but you take no damage.");
   }

   public void onActivate() {
      this.packets.clear();
   }

   public void onDeactivate() {
      if (this.mc.getNetworkHandler() != null && !this.packets.isEmpty()) {
         this.mc.getNetworkHandler().sendPacket((Packet)this.packets.get(this.packets.size() - 1));
      }

      this.packets.clear();
   }

   @EventHandler
   private void onPacketSend(Send event) {
      if (event.packet instanceof TeleportConfirmC2SPacket packet) {
         this.packets.add(packet);
         event.cancel();
      }
   }
}
