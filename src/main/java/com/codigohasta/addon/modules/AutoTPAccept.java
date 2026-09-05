package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class AutoTPAccept extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgDebug = this.settings.createGroup("andhigh");
   private final Setting<AutoTPAccept.Action> action = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Mode")).description("AutoConnectstillAutoReject.")).defaultValue(AutoTPAccept.Action.Accept))
            .build()
      );
   private final Setting<Boolean> onlyFriends = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Limitfriend"))
                  .description("OnlyLogic Meteor friendListinside'sPlayer."))
               .defaultValue(true))
            .build()
      );
   private final Setting<List<String>> keywords = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)new meteordevelopment.meteorclient.settings.StringListSetting.Builder()
                  .name("CheckTestKeyword"))
               .description("chatSkyOutPackthis'sDisappearTime, TriggerScan."))
            .defaultValue(new String[]{"Send", "tpa", "teleport"})
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Delay(Tick)"))
                  .description("toafterDelaymanyfew tick Logic."))
               .defaultValue(20))
            .min(0)
            .sliderMax(60)
            .build()
      );
   private final Setting<List<String>> customKeywords = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)new meteordevelopment.meteorclient.settings.StringListSetting.Builder()
                     .name("CustomCheckTestKeyword"))
                  .description("CustomModedownCheckTest'sKeywordList, anddownDirectionPointCommandOneOneto."))
               .defaultValue(new String[0])
               .visible(() -> this.action.get() == AutoTPAccept.Action.Custom))
            .build()
      );
   private final Setting<List<String>> customCommands = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)new meteordevelopment.meteorclient.settings.StringListSetting.Builder()
                     .name("CustomRowPointCommand"))
                  .description("CustomModedownRow'sPointCommandList, andupDirectionKeywordOneOneto. CheckTesttoKeyword[n]RowPointCommand[n]."))
               .defaultValue(new String[0])
               .visible(() -> this.action.get() == AutoTPAccept.Action.Custom))
            .build()
      );
   private final Setting<Boolean> debug = this.sgDebug
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Enable"))
                  .description("atchatSkyDisplayf***, Usecomebug."))
               .defaultValue(false))
            .build()
      );

   public AutoTPAccept() {
      super(
         AddonTemplate.CATEGORY,
         "AutoTPAccept",
         "Auto-accepts tpa from chat Connect/Reject prompts. On some spawn servers it auto-connects outgoing tpa and rejects incoming tpa."
      );
   }

   @EventHandler
   private void onReceivePacket(Receive event) {
      if (event.packet instanceof GameMessageS2CPacket packet) {
         Text textComponent = packet.content();
         String rawMessage = textComponent.getString();
         if ((Boolean)this.debug.get() && rawMessage.length() > 5) {
            this.info("[Debug-to]" + rawMessage, new Object[0]);
         }

         String targetCommand = null;
         if (this.action.get() == AutoTPAccept.Action.Custom) {
            List<String> customKwds = (List<String>)this.customKeywords.get();
            List<String> customCmds = (List<String>)this.customCommands.get();
            int minSize = Math.min(customKwds.size(), customCmds.size());

            for (int i = 0; i < minSize; i++) {
               if (rawMessage.contains(customKwds.get(i))) {
                  targetCommand = customCmds.get(i);
                  if ((Boolean)this.debug.get()) {
                     this.info("[Debug] CustomMode: \"" + customKwds.get(i) + "\" → \"" + targetCommand + "\"", new Object[0]);
                  }
                  break;
               }
            }

            if (targetCommand == null) {
               return;
            }
         } else {
            boolean hasKeyword = false;

            for (String k : (List)this.keywords.get()) {
               if (rawMessage.contains(k)) {
                  hasKeyword = true;
                  break;
               }
            }

            if (!hasKeyword) {
               return;
            }

            if ((Boolean)this.debug.get()) {
               this.info("[Debug] KeywordSuccess! startPointCommand...", new Object[0]);
            }

            List<String> foundCommands = new ArrayList<>();
            this.collectCommands(textComponent, foundCommands);
            if (foundCommands.isEmpty()) {
               if ((Boolean)this.debug.get()) {
                  this.warning("[Debug] DisappearinsidenottoCanpointStrike'sPointCommand! CancanareJSONtoo.", new Object[0]);
               }

               return;
            }

            if ((Boolean)this.debug.get()) {
               this.info("[Debug] to'shavePointCommand:" + foundCommands, new Object[0]);
            }

            for (String cmd : foundCommands) {
               String lower = cmd.toLowerCase();
               boolean isAccept = lower.contains("accept") || lower.contains("yes") || lower.contains("confirm");
               boolean isDeny = lower.contains("deny") || lower.contains("no") || lower.contains("cancel") || lower.contains("reject");
               if (this.action.get() == AutoTPAccept.Action.Accept && isAccept) {
                  targetCommand = cmd;
                  break;
               }

               if (this.action.get() == AutoTPAccept.Action.Deny && isDeny) {
                  targetCommand = cmd;
                  break;
               }
            }
         }

         if (targetCommand != null) {
            if ((Boolean)this.onlyFriends.get()) {
               boolean isFriend = false;

               for (Friend friend : Friends.get()) {
                  if (rawMessage.contains(friend.getName()) || targetCommand.contains(friend.getName())) {
                     isFriend = true;
                     break;
                  }
               }

               if (!isFriend) {
                  if ((Boolean)this.debug.get()) {
                     this.warning("[Debug] : Sendnotfriend.", new Object[0]);
                  }

                  return;
               }
            }

            String finalCmd = targetCommand;
            if ((Boolean)this.debug.get()) {
               this.info("[Debug] PrepareRow:" + finalCmd, new Object[0]);
            }

            if ((Integer)this.delay.get() > 0) {
               new Thread(() -> {
                  try {
                     Thread.sleep(((Integer)this.delay.get()).intValue() * 50L);
                     ChatUtils.sendPlayerMsg(finalCmd);
                  } catch (InterruptedException var3) {
                  }
               }).start();
            } else {
               ChatUtils.sendPlayerMsg(finalCmd);
            }

            if (this.action.get() == AutoTPAccept.Action.Custom) {
               this.info("AlreadyRowCustomPointCommand:" + targetCommand, new Object[0]);
            } else {
               this.info((this.action.get() == AutoTPAccept.Action.Accept ? "AlreadyConnect" : "AlreadyReject") + "TPA.", new Object[0]);
            }
         } else if ((Boolean)this.debug.get()) {
            this.warning("[Debug] topointStrikeThing, noMode(" + this.action.get() + ")'sPointCommand.", new Object[0]);
         }
      }
   }

   private void collectCommands(Text text, List<String> results) {
      Style style = text.getStyle();
      if (style != null && style.getClickEvent() != null) {
         ClickEvent click = style.getClickEvent();
         if (click.getAction() == net.minecraft.text.ClickEvent.Action.RUN_COMMAND
            || click.getAction() == net.minecraft.text.ClickEvent.Action.SUGGEST_COMMAND) {
            results.add(click.toString());
         }
      }

      for (Text sibling : text.getSiblings()) {
         this.collectCommands(sibling, results);
      }
   }

   public static enum Action {
      Accept("Connect"),
      Deny("Reject"),
      Custom("Custom");

      private final String title;

      private Action(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }
}
