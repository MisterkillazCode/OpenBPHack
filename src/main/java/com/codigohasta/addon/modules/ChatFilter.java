package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class ChatFilter extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<String>> badWords = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("BlockList")).description("CheckTesttothisTime, Other."))
               .defaultValue(
                  Arrays.asList(
                     "Operate",
                     "Scroll",
                     "Climb",
                     "f***",
                     "f***",
                     "idiot",
                     "than",
                     "idiot",
                     "sb",
                     "SB",
                     "retard",
                     "weak",
                     "moron",
                     "trash",
                     "retard",
                     "orphan",
                     "Spawn",
                     "dog",
                     "eat shit",
                     "momdead",
                     "youmom",
                     "your mom",
                     "diemom",
                     "nmsl",
                     "NMSL",
                     "cnm",
                     "CNM",
                     "tmd",
                     "TMD",
                     "nm",
                     "NM",
                     "wcnm",
                     "wdnmd",
                     "youmomdead",
                     "bastard",
                     "bitch",
                     "bastard",
                     "dieHome",
                     "Player",
                     "trash",
                     "youmom"
                  )
               ))
            .build()
      );
   private final Setting<String> replacementChar = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                     .name("Text"))
                  .description("UsecomeBlock's ( *)."))
               .defaultValue("*"))
            .build()
      );

   public ChatFilter() {
      super(AddonTemplate.CATEGORY, "ChatFilter", "Hides incoming chat messages containing blocked words, optionally replacing them with placeholders.");
   }

   @EventHandler
   private void onMessageReceive(ReceiveMessageEvent event) {
      if (this.mc.world != null && this.mc.player != null) {
         Text originalMessage = event.getMessage();
         String rawText = originalMessage.getString();
         if (this.containsBadWord(rawText)) {
            MutableText cleanMessage = Text.empty();
            List<String> sortedBadWords = new ArrayList<>((Collection<? extends String>)this.badWords.get());
            sortedBadWords.sort((s1, s2) -> s2.length() - s1.length());
            originalMessage.visit((style, asString) -> {
               String cleanPart = this.filterText(asString, sortedBadWords);
               cleanMessage.append(Text.literal(cleanPart).setStyle(style));
               return Optional.empty();
            }, Style.EMPTY);
            event.setMessage(cleanMessage);
         }
      }
   }

   private boolean containsBadWord(String text) {
      for (String word : (List)this.badWords.get()) {
         if (text.contains(word)) {
            return true;
         }
      }

      return false;
   }

   private String filterText(String text, List<String> sortedWords) {
      String result = text;
      String baseChar = (String)this.replacementChar.get();
      if (baseChar.isEmpty()) {
         baseChar = "*";
      }

      String singleChar = baseChar.substring(0, 1);

      for (String word : sortedWords) {
         if (result.contains(word)) {
            String stars = singleChar.repeat(word.length());
            result = result.replace(word, stars);
         }
      }

      return result;
   }
}
