package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ServerInfo;

public class AutoServer extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> initialDelay = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Delay (Tick)")).description("EnterServerafter, RowOnePointCommandbeforeWait'sTime."))
               .defaultValue(40))
            .min(0)
            .sliderMax(200)
            .build()
      );
   private final Setting<Integer> commandInterval = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("PointCommandBetween (Tick)")).description("Time to wait between each two commands (20 Tick = 1)."))
               .defaultValue(20))
            .min(0)
            .sliderMax(100)
            .build()
      );
   private final Setting<Boolean> showFeedback = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DisplayAnti"))
                  .description("DisplayPointCommandSendHintandDebug Info."))
               .defaultValue(true))
            .build()
      );
   private final SettingGroup sgServer1 = this.settings.createGroup("Server 1");
   private final Setting<Boolean> s1Enabled = this.sgServer1
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Enable"))
                  .description("whetherEnableServerConfig"))
               .defaultValue(true))
            .build()
      );
   private final Setting<String> s1Ip = this.sgServer1
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                     .name("Server IP"))
                  .description("TriggerConfig'sServerIP (: mcyanglao.com)"))
               .defaultValue("mcyanglao.com"))
            .build()
      );
   private final Setting<List<String>> s1Cmds = this.sgServer1
      .add(
         ((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)new meteordevelopment.meteorclient.settings.StringListSetting.Builder()
                     .name("RowPointCommand"))
                  .description("EnterServerafterRow'sPointCommand."))
               .defaultValue(List.of("login 123456", "server survival")))
            .build()
      );
   private final SettingGroup sgServer2 = this.settings.createGroup("Server 2");
   private final Setting<Boolean> s2Enabled = this.sgServer2
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Enable"))
               .defaultValue(false))
            .build()
      );
   private final Setting<String> s2Ip = this.sgServer2
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                  .name("Server IP"))
               .defaultValue("hypixel.net"))
            .build()
      );
   private final Setting<List<String>> s2Cmds = this.sgServer2
      .add(
         ((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)new meteordevelopment.meteorclient.settings.StringListSetting.Builder()
                  .name("RowPointCommand"))
               .defaultValue(List.of("play bedwars")))
            .build()
      );
   private final SettingGroup sgServer3 = this.settings.createGroup("Server 3");
   private final Setting<Boolean> s3Enabled = this.sgServer3
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Enable"))
               .defaultValue(false))
            .build()
      );
   private final Setting<String> s3Ip = this.sgServer3
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                  .name("Server IP"))
               .defaultValue("example.com"))
            .build()
      );
   private final Setting<List<String>> s3Cmds = this.sgServer3
      .add(
         ((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)new meteordevelopment.meteorclient.settings.StringListSetting.Builder()
                  .name("RowPointCommand"))
               .defaultValue(List.of("login password", "is")))
            .build()
      );
   private int timer = 0;
   private int currentCmdIndex = 0;
   private boolean isSending = false;
   private List<String> currentCmdList = null;
   private String processedSessionID = "NONE";

   public AutoServer() {
      super(AddonTemplate.CATEGORY, "AutoServer", "Runs a command automatically on server join; supports multiple servers with per-server command config.");
   }

   public void onActivate() {
      this.processedSessionID = "NONE";
      this.isSending = false;
      this.currentCmdList = null;
   }

   public void onDeactivate() {
      this.isSending = false;
      this.currentCmdList = null;
   }

   @EventHandler
   private void onTick(Pre event) {
      String currentSessionID = this.getSessionID();
      if (currentSessionID.equals("NONE")) {
         if (!this.processedSessionID.equals("NONE")) {
            this.processedSessionID = "NONE";
            this.isSending = false;
            this.currentCmdList = null;
            if ((Boolean)this.showFeedback.get()) {
               this.info("CheckTesttoOpenConnect, MemoryAlreadyReset.", new Object[0]);
            }
         }
      } else {
         if (!currentSessionID.equals(this.processedSessionID)) {
            if ((Boolean)this.showFeedback.get()) {
               this.info("CheckTesttoNewServerConnect:" + currentSessionID, new Object[0]);
            }

            this.processedSessionID = currentSessionID;
            if (!currentSessionID.equals("SINGLEPLAYER")) {
               List<String> matchedCommands = this.findMatchingCommands(currentSessionID);
               if (matchedCommands != null && !matchedCommands.isEmpty()) {
                  this.startSequence(matchedCommands);
                  if ((Boolean)this.showFeedback.get()) {
                     this.info("toConfig, at" + this.initialDelay.get() + "tick afterSend" + matchedCommands.size() + "PointCommand", new Object[0]);
                  }
               } else {
                  this.isSending = false;
                  this.currentCmdList = null;
                  if ((Boolean)this.showFeedback.get()) {
                     this.info("to'sEnableConfigPointCommandListForAir.", new Object[0]);
                  }
               }
            } else {
               this.isSending = false;
               this.currentCmdList = null;
            }
         }

         if (this.isSending && this.currentCmdList != null) {
            if (this.mc.player != null && this.mc.world != null) {
               if (this.timer > 0) {
                  this.timer--;
               } else if (this.currentCmdIndex >= this.currentCmdList.size()) {
                  if ((Boolean)this.showFeedback.get() && this.currentCmdIndex > 0) {
                     this.info("PointCommandRowComplete.", new Object[0]);
                  }

                  this.isSending = false;
                  this.currentCmdList = null;
               } else {
                  String cmd = this.currentCmdList.get(this.currentCmdIndex);
                  if (cmd != null && !cmd.trim().isEmpty()) {
                     this.sendCommand(cmd);
                     if ((Boolean)this.showFeedback.get()) {
                        this.info("AutoSend:" + cmd, new Object[0]);
                     }
                  }

                  this.currentCmdIndex++;
                  if (this.currentCmdIndex < this.currentCmdList.size()) {
                     this.timer = (Integer)this.commandInterval.get();
                  }
               }
            }
         }
      }
   }

   private void startSequence(List<String> cmdList) {
      this.isSending = true;
      this.currentCmdList = cmdList;
      this.currentCmdIndex = 0;
      this.timer = (Integer)this.initialDelay.get();
   }

   private List<String> findMatchingCommands(String currentIp) {
      if ((Boolean)this.s1Enabled.get() && this.isValidIp((String)this.s1Ip.get(), currentIp)) {
         return (List<String>)this.s1Cmds.get();
      } else if ((Boolean)this.s2Enabled.get() && this.isValidIp((String)this.s2Ip.get(), currentIp)) {
         return (List<String>)this.s2Cmds.get();
      } else {
         return this.s3Enabled.get() && this.isValidIp((String)this.s3Ip.get(), currentIp) ? (List)this.s3Cmds.get() : null;
      }
   }

   private boolean isValidIp(String settingIp, String currentIp) {
      String trimIp = settingIp.trim().toLowerCase();
      return !trimIp.isEmpty() && currentIp.contains(trimIp);
   }

   private String getSessionID() {
      if (this.mc.isInSingleplayer()) {
         return "SINGLEPLAYER";
      } else {
         ServerInfo info = this.mc.getCurrentServerEntry();
         return info == null ? "NONE" : info.address.toLowerCase();
      }
   }

   private void sendCommand(String cmd) {
      String cleanCmd = cmd.trim();
      if (cleanCmd.startsWith("/")) {
         cleanCmd = cleanCmd.substring(1);
      }

      if (this.mc.player != null && this.mc.player.networkHandler != null) {
         this.mc.player.networkHandler.sendChatCommand(cleanCmd);
      }
   }
}
