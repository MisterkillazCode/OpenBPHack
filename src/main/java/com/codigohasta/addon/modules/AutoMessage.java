package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

public class AutoMessage extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<String>> messages = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("messages")).name("PointCommandList"))
                  .description(
                     "PointCommandList. Enable'OnlyRowSelectin', atwantRow'sPointCommandbeforebefore. \nMethod: [+before] [insideTolerate] #[DelayNumber]\n: '+ /spawn #5' (SendafterWait5)"
                  ))
               .defaultValue(List.of("+ TestDisappearyougood #3", "1 #60", "+ 3 #5", "Test #30")))
            .build()
      );
   private final Setting<Integer> defaultDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("default-delay"))
                     .name("DefaultDelay ()"))
                  .description("PointSetDelayTimeUse'sDefaultWaitTime (SingleBit: )."))
               .defaultValue(3))
            .min(1)
            .sliderMax(300)
            .build()
      );
   private final Setting<Boolean> loop = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("loop"))
                     .name("Mode"))
                  .description("ListRowCompleteafterwhetherHeavyNewstart."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> random = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("random"))
                     .name("Random"))
                  .description("fromhaveEffectListinRandomSelectPointCommandSend."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> onlySelected = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("only-selected"))
                     .name("OnlyRowSelectin"))
                  .description("Enableafter, Onlyhave'Selectinbefore'OpenHead'sPointCommandwillbySend."))
               .defaultValue(false))
            .build()
      );
   private final Setting<String> selectPrefix = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                           .name("select-prefix"))
                        .name("Selectinbefore"))
                     .description("UseMarkbySelectinPointCommand's."))
                  .defaultValue("+"))
               .visible(this.onlySelected::get))
            .build()
      );
   private int timer = 0;
   private int messageIndex = 0;

   public AutoMessage() {
      super(AddonTemplate.CATEGORY, "AutoMessage", "Sends chat messages or commands on a loop with configurable delays and random selection.");
   }

   public void onActivate() {
      this.timer = 0;
      this.messageIndex = 0;
   }

   @EventHandler
   private void onTick(Pre event) {
      List<String> allMessages = (List<String>)this.messages.get();
      if (!allMessages.isEmpty()) {
         List<String> validList = new ArrayList<>();
         String prefix = (String)this.selectPrefix.get();

         for (String line : allMessages) {
            if ((Boolean)this.onlySelected.get()) {
               if (line.startsWith(prefix)) {
                  validList.add(line);
               }
            } else if (!line.trim().isEmpty()) {
               validList.add(line);
            }
         }

         if (!validList.isEmpty()) {
            if (this.timer > 0) {
               this.timer--;
            } else {
               this.sendMessage(validList);
            }
         }
      }
   }

   private void sendMessage(List<String> validList) {
      if (this.messageIndex >= validList.size()) {
         if (!(Boolean)this.loop.get()) {
            this.toggle();
            return;
         }

         this.messageIndex = 0;
      }

      String rawLine;
      if ((Boolean)this.random.get()) {
         rawLine = validList.get((int)(Math.random() * validList.size()));
      } else {
         rawLine = validList.get(this.messageIndex);
      }

      if ((Boolean)this.onlySelected.get() && rawLine.startsWith((String)this.selectPrefix.get())) {
         rawLine = rawLine.substring(((String)this.selectPrefix.get()).length()).trim();
      }

      String content = rawLine;
      int delaySeconds = (Integer)this.defaultDelay.get();
      if (rawLine.contains("#")) {
         int hashIndex = rawLine.lastIndexOf("#");
         String potentialDelay = rawLine.substring(hashIndex + 1).trim();

         try {
            int parsedDelay = Integer.parseInt(potentialDelay);
            delaySeconds = parsedDelay;
            content = rawLine.substring(0, hashIndex).trim();
         } catch (NumberFormatException var8) {
         }
      }

      if (!content.isEmpty()) {
         ChatUtils.sendPlayerMsg(content);
      }

      this.timer = delaySeconds * 20;
      if (!(Boolean)this.random.get()) {
         this.messageIndex++;
      }
   }
}
