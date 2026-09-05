package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChatPrefixCustom extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgStyle = this.settings.createGroup("Style");
   private final Setting<String> prefixText = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("prefix-text")).description("Prefix text, without the square brackets.")).defaultValue("BPHack"))
            .build()
      );
   private final Setting<ChatPrefixCustom.ColorMode> colorMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("color-mode"))
               .defaultValue(ChatPrefixCustom.ColorMode.Fixed))
            .build()
      );
   private final Setting<SettingColor> prefixColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("prefix-color"))
            .defaultValue(new SettingColor(85, 200, 255, 255))
            .build()
      );
   private final Setting<SettingColor> prefixColorEnd = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("prefix-color-end"))
               .defaultValue(new SettingColor(120, 120, 255, 255))
               .visible(() -> this.colorMode.get() == ChatPrefixCustom.ColorMode.Gradient))
            .build()
      );
   private final Setting<Boolean> bold = this.sgStyle
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("bold"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> italic = this.sgStyle
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("italic"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> underline = this.sgStyle
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("underline"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> strikethrough = this.sgStyle
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("strikethrough"))
               .defaultValue(false))
            .build()
      );

   public ChatPrefixCustom() {
      super(
         AddonTemplate.CATEGORY,
         "ChatPrefixCustom",
         "Adds a custom chat prefix (e.g. [BPHack]) to your outgoing messages, with gradient and bold/italic support."
      );
   }

   public static Text defaultPrefix() {
      return Text.literal("[")
         .formatted(Formatting.GRAY)
         .append(Text.literal("BPHack").formatted(Formatting.AQUA))
         .append(Text.literal("] ").formatted(Formatting.GRAY));
   }

   public static void registerDefault() {
      ChatUtils.registerCustomPrefix("com.codigohasta.addon", ChatPrefixCustom::defaultPrefix);
   }

   public void onActivate() {
      ChatUtils.registerCustomPrefix("com.codigohasta.addon", this::buildPrefix);
      this.info("§a[√] BPHack 前缀已启用", new Object[0]);
   }

   public void onDeactivate() {
      ChatUtils.unregisterCustomPrefix("com.codigohasta.addon");
      ChatUtils.registerCustomPrefix("com.codigohasta.addon", ChatPrefixCustom::defaultPrefix);
   }

   private Text buildPrefix() {
      MutableText prefix = Text.literal("[").formatted(Formatting.GRAY);
      String text = (String)this.prefixText.get();
      if (text.isEmpty()) {
         text = "BPHack";
      }

      for (int i = 0; i < text.length(); i++) {
         char ch = text.charAt(i);
         MutableText chText = Text.literal(String.valueOf(ch));
         int color;
         if (this.colorMode.get() == ChatPrefixCustom.ColorMode.Gradient && text.length() > 1) {
            float progress = (float)i / (text.length() - 1);
            color = this.getGradientColor((SettingColor)this.prefixColor.get(), (SettingColor)this.prefixColorEnd.get(), progress);
         } else {
            color = ((SettingColor)this.prefixColor.get()).getPacked();
         }

         Style style = Style.EMPTY
            .withColor(color)
            .withBold((Boolean)this.bold.get())
            .withItalic((Boolean)this.italic.get())
            .withUnderline((Boolean)this.underline.get())
            .withStrikethrough((Boolean)this.strikethrough.get());
         chText.setStyle(style);
         prefix.append(chText);
      }

      prefix.append(Text.literal("] ").formatted(Formatting.GRAY));
      return prefix;
   }

   private int getGradientColor(SettingColor start, SettingColor end, float progress) {
      int r = this.interpolate(start.r, end.r, progress);
      int g = this.interpolate(start.g, end.g, progress);
      int b = this.interpolate(start.b, end.b, progress);
      int a = this.interpolate(start.a, end.a, progress);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private int interpolate(int start, int end, float progress) {
      return (int)(start + (end - start) * progress);
   }

   public static enum ColorMode {
      Fixed,
      Gradient;
   }
}
