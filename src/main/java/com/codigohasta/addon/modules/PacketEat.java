package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;

public class PacketEat extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> deSync = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("DeSync"))
                  .description("Re-sends the interact item packet every tick while eating, desyncing the server's view of your eating progress (anti Grim)."))
               .defaultValue(false))
            .build()
      );

   public PacketEat() {
      super(
         AddonTemplate.CATEGORY,
         "PacketEat",
         "Cancels the release-use-item packet while eating so the server never sees you finish, and optionally desyncs eating. Bypasses food-related Grim checks."
      );
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null) {
         if ((Boolean)this.deSync.get()
            && this.mc.player.isUsingItem()
            && this.mc.player.getActiveItem().get(DataComponentTypes.CONSUMABLE) != null) {
            this.sendSequencedPacket(
               id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, this.mc.player.getYaw(), this.mc.player.getPitch())
            );
         }
      }
   }

   @EventHandler
   private void onPacketSent(Send event) {
      if (event.packet instanceof PlayerActionC2SPacket packet
         && packet.getAction() == Action.RELEASE_USE_ITEM
         && this.mc.player != null
         && this.mc.player.isUsingItem()
         && this.mc.player.getActiveItem().get(DataComponentTypes.CONSUMABLE) != null) {
         event.cancel();
      }
   }

   public void sendSequencedPacket(SequencedPacketCreator packetCreator) {
      if (this.mc.getNetworkHandler() != null && this.mc.world != null) {
         PendingUpdateManager pendingUpdateManager = this.mc.world.getPendingUpdateManager().incrementSequence();

         try {
            int i = pendingUpdateManager.getSequence();
            this.mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
         } catch (Throwable var6) {
            if (pendingUpdateManager != null) {
               try {
                  pendingUpdateManager.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (pendingUpdateManager != null) {
            pendingUpdateManager.close();
         }
      }
   }
}
