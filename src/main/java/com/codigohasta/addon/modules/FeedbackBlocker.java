package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.Arrays;
import java.util.List;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;

public class FeedbackBlocker extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> ignoreCase = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Size")).description("Enableafter, 'placed' alsocanBlock 'Placed'.")).defaultValue(true)).build());
   private final Setting<List<String>> keywords = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)new meteordevelopment.meteorclient.settings.StringListSetting.Builder()
                     .name("BlockKeyword"))
                  .description("DisappearinCompletePackthisshortTimeProceed."))
               .defaultValue(Arrays.asList("Global Auctionstart", "moreBit", "AlreadySuccess", "New's", "Placed", "Undo", "Affected", "Paste", "Schematic")))
            .build()
      );

   public FeedbackBlocker() {
      super(AddonTemplate.CATEGORY, "FeedbackBlocker", "Hides server feedback messages containing specified keywords.");
   }

   @EventHandler
   private void onMessageReceive(ReceiveMessageEvent event) {
      if (this.mc.world != null && this.mc.player != null) {
         Text message = event.getMessage();
         String content = message.getString();
         String contentToCheck = this.ignoreCase.get() ? content.toLowerCase() : content;

         for (String keyword : (List)this.keywords.get()) {
            if (!keyword.isEmpty()) {
               String keywordToCheck = this.ignoreCase.get() ? keyword.toLowerCase() : keyword;
               if (contentToCheck.contains(keywordToCheck)) {
                  event.cancel();
                  return;
               }
            }
         }
      }
   }
}
