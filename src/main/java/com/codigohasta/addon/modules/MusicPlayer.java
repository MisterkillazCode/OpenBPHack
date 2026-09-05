package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public class MusicPlayer extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<MusicPlayer.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("RowMode")).description("SelectpointMechanism'sDoDirection."))
               .defaultValue(MusicPlayer.Mode.Sequential))
            .build()
      );
   private final Setting<Double> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("SendBetween ()"))
                  .description("manyfewSenddownOnePointCommand."))
               .defaultValue(2.0)
               .min(0.1)
               .sliderMax(10.0)
               .visible(() -> this.mode.get() == MusicPlayer.Mode.Sequential || this.mode.get() == MusicPlayer.Mode.Random))
            .build()
      );
   private final Setting<Integer> currentLine = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("currentRow (EnterDegree)"))
                     .description("currentPuttoRow (CanManual)."))
                  .defaultValue(1))
               .min(1)
               .noSlider()
               .visible(() -> this.mode.get() == MusicPlayer.Mode.Sequential))
            .build()
      );
   private final Setting<String> triggerPrefix = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                        .name("pointTriggerbefore"))
                     .description("chatSkydon't'sbefore (: #point 1)."))
                  .defaultValue("#"))
               .visible(() -> this.mode.get() == MusicPlayer.Mode.Request))
            .build()
      );
   private final Setting<Boolean> listenToOthers = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("AllowhePlayerpoint"))
                     .description("whetherOtherPlayerSend'spointPointCommand."))
                  .defaultValue(true))
               .visible(() -> this.mode.get() == MusicPlayer.Mode.Request))
            .build()
      );
   private File file = null;
   private final PointerBuffer filters;
   private final List<String> allLines = new ArrayList<>();
   private final Random random = new Random();
   private int timer;

   public MusicPlayer() {
      super(
         AddonTemplate.CATEGORY,
         "MusicPlayer",
         "Uses /music auto-point; you can add your own single-line text. Only auto-points; some servers have a /music plugin."
      );
      this.filters = BufferUtils.createPointerBuffer(1);
      ByteBuffer txtFilter = MemoryUtil.memASCII("*.txt");
      this.filters.put(txtFilter);
      this.filters.rewind();
   }

   public WWidget getWidget(GuiTheme theme) {
      WVerticalList root = theme.verticalList();
      WHorizontalList fileList = (WHorizontalList)root.add(theme.horizontalList()).widget();
      WButton selectFile = (WButton)fileList.add(theme.button("SelectSingleText")).widget();
      WLabel fileNameLabel = (WLabel)fileList.add(theme.label(this.file != null && this.file.exists() ? this.file.getName() : "currentSelectText")).widget();
      WHorizontalList controlList = (WHorizontalList)root.add(theme.horizontalList()).widget();
      WButton resetProgress = (WButton)controlList.add(theme.button("ResetPutEnterDegree (1)")).widget();
      selectFile.action = () -> {
         String initialPath = this.file != null ? this.file.getAbsolutePath() : new File(MeteorClient.FOLDER, "songs.txt").getAbsolutePath();
         String path = TinyFileDialogs.tinyfd_openFileDialog("SelectSingleText (.txt)", initialPath, this.filters, "Text Files", false);
         if (path != null) {
            File newFile = new File(path);
            if (newFile.exists()) {
               this.file = newFile;
               fileNameLabel.set(this.file.getName());
               this.loadFile();
               this.currentLine.set(1);
               this.info("AlreadySwitchText, EnterDegreeAlreadyResetFor 1 .", new Object[0]);
            }
         }
      };
      resetProgress.action = () -> {
         this.currentLine.set(1);
         this.info("PutEnterDegreeAlreadyResetFor 1.", new Object[0]);
      };
      return root;
   }

   public void onActivate() {
      if (this.file != null && this.file.exists()) {
         this.loadFile();
         if ((Integer)this.currentLine.get() > this.allLines.size()) {
            this.warning("ofbefore'sEnterDegree (" + this.currentLine.get() + ") OutcurrentTextRowNumber, AlreadyResetFor 1.", new Object[0]);
            this.currentLine.set(1);
         }

         this.timer = (int)((Double)this.delay.get() * 20.0);
         if (this.mode.get() == MusicPlayer.Mode.Sequential) {
            this.info("startPut, from" + this.currentLine.get() + "Rowstart.", new Object[0]);
         }
      } else {
         this.error("SelectText! OpenSettingSelectText.", new Object[0]);
         this.toggle();
      }
   }

   private void loadFile() {
      this.allLines.clear();

      String line;
      try (BufferedReader br = new BufferedReader(new FileReader(this.file))) {
         while ((line = br.readLine()) != null) {
            if (!line.trim().isEmpty()) {
               this.allLines.add(line.trim());
            }
         }
      } catch (IOException var6) {
         this.error("Text:" + var6.getMessage(), new Object[0]);
         if (this.isActive()) {
            this.toggle();
         }
      }

      if (this.allLines.isEmpty()) {
         this.error("TextareAir's!", new Object[0]);
         if (this.isActive()) {
            this.toggle();
         }
      }
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mode.get() != MusicPlayer.Mode.Request) {
         if (this.timer <= 0) {
            if (this.mode.get() == MusicPlayer.Mode.Sequential) {
               int lineNum = (Integer)this.currentLine.get();
               int index = lineNum - 1;
               if (index >= this.allLines.size()) {
                  this.info("ListPutComplete (" + this.allLines.size() + "), AutoDisable.", new Object[0]);
                  this.toggle();
                  return;
               }

               this.sendCommand(this.allLines.get(index));
               this.currentLine.set(lineNum + 1);
            } else if (this.mode.get() == MusicPlayer.Mode.Random) {
               if (this.allLines.isEmpty()) {
                  return;
               }

               int randomIndex = this.random.nextInt(this.allLines.size());
               this.sendCommand(this.allLines.get(randomIndex));
            }

            this.timer = (int)((Double)this.delay.get() * 20.0);
         } else {
            this.timer--;
         }
      }
   }

   @EventHandler
   private void onReceiveMessage(ReceiveMessageEvent event) {
      if (this.mode.get() == MusicPlayer.Mode.Request) {
         String message = event.getMessage().getString();
         String prefix = (String)this.triggerPrefix.get();
         if (message.contains(prefix)) {
            try {
               int indexInMsg = message.indexOf(prefix);
               String temp = message.substring(indexInMsg + prefix.length()).trim();
               String[] parts = temp.split("\\s+");
               if (parts.length == 0) {
                  return;
               }

               String numberStr = parts[0].replaceAll("[^0-9]", "");
               if (numberStr.isEmpty()) {
                  return;
               }

               int lineNumber = Integer.parseInt(numberStr);
               boolean isMe = false;
               if (this.mc.player != null && message.contains(this.mc.player.getName().getString())) {
                  isMe = true;
               }

               if (!(Boolean)this.listenToOthers.get() && !isMe) {
                  return;
               }

               this.executeRequest(lineNumber);
            } catch (Exception var10) {
            }
         }
      }
   }

   private void executeRequest(int lineNum) {
      int index = lineNum - 1;
      if (index >= 0 && index < this.allLines.size()) {
         this.info("point:" + lineNum + "Row.", new Object[0]);
         this.sendCommand(this.allLines.get(index));
      }
   }

   private void sendCommand(String cmd) {
      if (this.mc.player != null) {
         if (cmd.startsWith("/")) {
            this.mc.getNetworkHandler().sendChatCommand(cmd.substring(1));
         } else {
            this.mc.getNetworkHandler().sendChatMessage(cmd);
         }
      }
   }

   public NbtCompound toTag() {
      NbtCompound tag = super.toTag();
      if (this.file != null && this.file.exists()) {
         tag.putString("file", this.file.getAbsolutePath());
      }

      return tag;
   }

   public Module fromTag(NbtCompound tag) {
      if (tag.contains("file")) {
         this.file = new File(tag.getString("file").orElse(""));
      }

      return super.fromTag(tag);
   }

   public static enum Mode {
      Sequential,
      Request,
      Random;
   }
}
