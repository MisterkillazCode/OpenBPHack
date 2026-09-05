package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public class ChatHighlight extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgSelf = this.settings.createGroup("yourself (Self)");
   private final SettingGroup sgManager = this.settings.createGroup("NameSingleLogic (Manager)");
   private final SettingGroup sgOthers = this.settings.createGroup("PathPlayer (Others)");
   private final Map<String, SettingColor> specificPlayers = new HashMap<>();
   private final Setting<Boolean> strictMode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Mode")).description("Enableafter, OnlyComplete'sName. \n: Name 'God' Time, notwill 'Godzilla'."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> highlightSelf = this.sgSelf
      .add(((Builder)((Builder)((Builder)new Builder().name("highyourself")).description("whetherhighDisplayyourself'schatSky.")).defaultValue(true)).build());
   private final Setting<Boolean> onlySender = this.sgSelf
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("AsSendTime"))
                  .description(
                     "RecommendEnable! \nOnlyhaveyourNameOutnowDisappearOpenHead(CancanareyouSend's)thenColor. \nDefenseStopdon'tPlayertoyou(@you)TimealsoCompleteyourColor."
                  ))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> selfColor = this.sgSelf
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Color"))
               .defaultValue(new SettingColor(0, 255, 255))
               .visible(this.highlightSelf::get))
            .build()
      );
   private final Setting<Boolean> selfBold = this.sgSelf
      .add(((Builder)((Builder)((Builder)new Builder().name("Bold (Bold)")).defaultValue(true)).visible(this.highlightSelf::get)).build());
   private final Setting<Boolean> selfItalic = this.sgSelf
      .add(((Builder)((Builder)((Builder)new Builder().name("Italic (Italic)")).defaultValue(false)).visible(this.highlightSelf::get)).build());
   private final Setting<Boolean> selfUnderline = this.sgSelf
      .add(((Builder)((Builder)((Builder)new Builder().name("downLine (Underline)")).defaultValue(false)).visible(this.highlightSelf::get)).build());
   private final Setting<Boolean> selfStrikethrough = this.sgSelf
      .add(((Builder)((Builder)((Builder)new Builder().name("DeleteLine (Strikethrough)")).defaultValue(false)).visible(this.highlightSelf::get)).build());
   private final Setting<String> inputName = this.sgManager
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                     .name("Target PlayerID"))
                  .description("wantAdd, Delete'sPlayerName."))
               .defaultValue(""))
            .build()
      );
   private final Setting<SettingColor> inputColor = this.sgManager
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("SettingColor"))
               .description("ForupDirection'sPlayerSelectOneColor."))
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build()
      );
   private final Setting<Boolean> btnAdd = this.sgManager
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("Add / UpdateNameSingle")).description("pointStrike [Name+Color] SaveEnterhighNameSingle."))
                  .defaultValue(false))
               .onChanged(v -> {
                  if (v) {
                     this.addEntry();
                  }
               }))
            .build()
      );
   private final Setting<Boolean> btnRemove = this.sgManager
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("fromNameSingleRemove")).description("pointStrike [Name] fromNameSingleinDelete."))
                  .defaultValue(false))
               .onChanged(v -> {
                  if (v) {
                     this.removeEntry();
                  }
               }))
            .build()
      );
   private final Setting<Boolean> btnPrint = this.sgManager
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("HitcurrentNameSingle")).description("atchatSkyDisplayhaveAlreadyConfig'sSetPlayer."))
                  .defaultValue(false))
               .onChanged(v -> {
                  if (v) {
                     this.printList();
                  }
               }))
            .build()
      );
   private final Setting<Boolean> specificBold = this.sgManager.add(((Builder)((Builder)new Builder().name("NameSingle-Bold")).defaultValue(true)).build());
   private final Setting<Boolean> specificItalic = this.sgManager
      .add(((Builder)((Builder)new Builder().name("NameSingle-Italic")).defaultValue(false)).build());
   private final Setting<Boolean> specificUnderline = this.sgManager
      .add(((Builder)((Builder)new Builder().name("NameSingle-downLine")).defaultValue(false)).build());
   private final Setting<Boolean> specificStrikethrough = this.sgManager
      .add(((Builder)((Builder)new Builder().name("NameSingle-DeleteLine")).defaultValue(false)).build());
   private final Setting<Boolean> highlightOthers = this.sgOthers
      .add(((Builder)((Builder)((Builder)new Builder().name("highPathPlayer")).description("whetherOtherSetPlayer'schatSkyColor.")).defaultValue(true)).build());
   private final Setting<String> othersPrefix = this.sgOthers
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                        .name("beforeinsideTolerate"))
                     .description("givePathPlayerDisappearAddbefore, '[PathPlayer] '. AirnotDisplay."))
                  .defaultValue(""))
               .visible(this.highlightOthers::get))
            .build()
      );
   private final Setting<SettingColor> othersPrefixColor = this.sgOthers
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("beforeColor"))
               .defaultValue(new SettingColor(150, 150, 150))
               .visible(this.highlightOthers::get))
            .build()
      );
   private final Setting<Boolean> prefixBold = this.sgOthers
      .add(((Builder)((Builder)((Builder)new Builder().name("before-Bold")).defaultValue(false)).visible(this.highlightOthers::get)).build());
   private final Setting<SettingColor> othersColor = this.sgOthers
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("TextColor"))
               .defaultValue(new SettingColor(255, 170, 0))
               .visible(this.highlightOthers::get))
            .build()
      );
   private final Setting<Boolean> othersBold = this.sgOthers
      .add(((Builder)((Builder)((Builder)new Builder().name("Text-Bold")).defaultValue(false)).visible(this.highlightOthers::get)).build());
   private final Setting<Boolean> othersItalic = this.sgOthers
      .add(((Builder)((Builder)((Builder)new Builder().name("Text-Italic")).defaultValue(false)).visible(this.highlightOthers::get)).build());
   private final Setting<Boolean> othersUnderline = this.sgOthers
      .add(((Builder)((Builder)((Builder)new Builder().name("Text-downLine")).defaultValue(false)).visible(this.highlightOthers::get)).build());
   private final Setting<Boolean> othersStrikethrough = this.sgOthers
      .add(((Builder)((Builder)((Builder)new Builder().name("Text-DeleteLine")).defaultValue(false)).visible(this.highlightOthers::get)).build());

   public ChatHighlight() {
      super(AddonTemplate.CATEGORY, "ChatHighlight", "Highlights chat messages from specific players with a custom colour and text styles.");
   }

   @EventHandler
   private void onMessageReceive(ReceiveMessageEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         String textContent = event.getMessage().getString();
         String myName = this.mc.player.getName().getString();
         if ((Boolean)this.highlightSelf.get()) {
            boolean isSelfMessage = false;
            if (this.isWordPresent(textContent, myName)) {
               if ((Boolean)this.onlySender.get()) {
                  int index = textContent.indexOf(myName);
                  if (index >= 0 && index < 25) {
                     String prefixStr = textContent.substring(0, index);
                     if (!prefixStr.contains("@")) {
                        isSelfMessage = true;
                     }
                  }
               } else {
                  isSelfMessage = true;
               }
            }

            if (isSelfMessage) {
               this.modifyMessage(
                  event,
                  (SettingColor)this.selfColor.get(),
                  (Boolean)this.selfBold.get(),
                  (Boolean)this.selfItalic.get(),
                  (Boolean)this.selfUnderline.get(),
                  (Boolean)this.selfStrikethrough.get()
               );
               return;
            }
         }

         for (Entry<String, SettingColor> entry : this.specificPlayers.entrySet()) {
            String targetName = entry.getKey();
            if (this.isWordPresent(textContent, targetName)) {
               this.modifyMessage(
                  event,
                  entry.getValue(),
                  (Boolean)this.specificBold.get(),
                  (Boolean)this.specificItalic.get(),
                  (Boolean)this.specificUnderline.get(),
                  (Boolean)this.specificStrikethrough.get()
               );
               return;
            }
         }

         if ((Boolean)this.highlightOthers.get() && this.isFromPlayer(textContent)) {
            this.addPrefixAndModify(event);
         }
      }
   }

   private boolean isWordPresent(String text, String name) {
      if (name == null || name.isEmpty()) {
         return false;
      } else if ((Boolean)this.strictMode.get()) {
         String regex = "(?i).*\\b" + Pattern.quote(name) + "\\b.*";
         return text.matches(regex);
      } else {
         return text.contains(name);
      }
   }

   private boolean isFromPlayer(String message) {
      for (PlayerListEntry entry : this.mc.getNetworkHandler().getPlayerList()) {
         String pName = entry.getProfile().name();
         if (!pName.equals(this.mc.player.getName().getString()) && this.isWordPresent(message, pName)) {
            return true;
         }
      }

      return false;
   }

   private void modifyMessage(ReceiveMessageEvent event, SettingColor color, boolean bold, boolean italic, boolean underline, boolean strikethrough) {
      MutableText newMessage = event.getMessage().copy();
      Style style = newMessage.getStyle()
         .withColor(TextColor.fromRgb(color.getPacked()))
         .withBold(bold)
         .withItalic(italic)
         .withUnderline(underline)
         .withStrikethrough(strikethrough);
      newMessage.setStyle(style);
      event.setMessage(newMessage);
   }

   private void addPrefixAndModify(ReceiveMessageEvent event) {
      MutableText body = event.getMessage().copy();
      Style bodyStyle = body.getStyle()
         .withColor(TextColor.fromRgb(((SettingColor)this.othersColor.get()).getPacked()))
         .withBold((Boolean)this.othersBold.get())
         .withItalic((Boolean)this.othersItalic.get())
         .withUnderline((Boolean)this.othersUnderline.get())
         .withStrikethrough((Boolean)this.othersStrikethrough.get());
      body.setStyle(bodyStyle);
      String pText = (String)this.othersPrefix.get();
      if (pText != null && !pText.isEmpty()) {
         MutableText prefix = Text.literal(pText);
         Style prefixStyle = Style.EMPTY
            .withColor(TextColor.fromRgb(((SettingColor)this.othersPrefixColor.get()).getPacked()))
            .withBold((Boolean)this.prefixBold.get());
         prefix.setStyle(prefixStyle);
         prefix.append(body);
         event.setMessage(prefix);
      } else {
         event.setMessage(body);
      }
   }

   private void addEntry() {
      String name = ((String)this.inputName.get()).trim();
      if (name.isEmpty()) {
         ChatUtils.error("NamenotcanForAir!", new Object[0]);
         this.btnAdd.set(false);
      } else {
         SettingColor color = new SettingColor((SettingColor)this.inputColor.get());
         this.specificPlayers.put(name, color);
         ChatUtils.info("AlreadyAdd/UpdateSetPlayer:" + name, new Object[0]);
         this.btnAdd.set(false);
      }
   }

   private void removeEntry() {
      String name = ((String)this.inputName.get()).trim();
      if (this.specificPlayers.containsKey(name)) {
         this.specificPlayers.remove(name);
         ChatUtils.info("AlreadyRemovePlayer:" + name, new Object[0]);
      } else {
         ChatUtils.error("ListinnottoPlayer:" + name, new Object[0]);
      }

      this.btnRemove.set(false);
   }

   private void printList() {
      if (this.specificPlayers.isEmpty()) {
         ChatUtils.info("SetPlayer ListForAir.", new Object[0]);
      } else {
         ChatUtils.info("=== SethighNameSingle (" + this.specificPlayers.size() + ") ===", new Object[0]);

         for (Entry<String, SettingColor> entry : this.specificPlayers.entrySet()) {
            MutableText nameText = Text.literal(" - " + entry.getKey());
            nameText.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(entry.getValue().getPacked())));
            this.mc.inGameHud.getChatHud().addMessage(nameText);
         }
      }

      this.btnPrint.set(false);
   }

   public NbtCompound toTag() {
      NbtCompound tag = super.toTag();
      NbtList list = new NbtList();

      for (Entry<String, SettingColor> entry : this.specificPlayers.entrySet()) {
         NbtCompound entryTag = new NbtCompound();
         entryTag.putString("name", entry.getKey());
         SettingColor c = entry.getValue();
         entryTag.putInt("r", c.r);
         entryTag.putInt("g", c.g);
         entryTag.putInt("b", c.b);
         entryTag.putInt("a", c.a);
         list.add(entryTag);
      }

      tag.put("specificPlayers", list);
      return tag;
   }

   public Module fromTag(NbtCompound tag) {
      super.fromTag(tag);
      if (tag.contains("specificPlayers")) {
         this.specificPlayers.clear();

         for (NbtElement element : (NbtList)tag.getList("specificPlayers").orElse(null)) {
            NbtCompound entryTag = (NbtCompound)element;
            String name = entryTag.getString("name").orElse("");
            int r = entryTag.getInt("r").orElse(255);
            int g = entryTag.getInt("g").orElse(255);
            int b = entryTag.getInt("b").orElse(255);
            int a = entryTag.getInt("a").orElse(255);
            this.specificPlayers.put(name, new SettingColor(r, g, b, a));
         }
      }

      return this;
   }
}
