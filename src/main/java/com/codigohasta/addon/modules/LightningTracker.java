package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.math.Vec3d;

public class LightningTracker extends Module {
   public LightningTracker() {
      super(
         AddonTemplate.SC_CATEGORY,
         "LightningTracker",
         "Logs the coordinates and distance of every lightning bolt the server spawns. For self-hosted / LAN PvP only."
      );
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      if (this.mc.player != null) {
         if (event.packet instanceof EntitySpawnS2CPacket packet && packet.getEntityType() == EntityType.LIGHTNING_BOLT) {
            int x = (int)packet.getX();
            int y = (int)packet.getY();
            int z = (int)packet.getZ();
            double dist = this.mc.player.getSyncedPos().distanceTo(new Vec3d(x + 0.5, y, z + 0.5));
            ChatUtils.info("Lightning struck at " + x + ", " + y + ", " + z + " (" + (int)dist + " blocks away)", new Object[0]);
         }
      }
   }
}
