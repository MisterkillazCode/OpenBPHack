package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.awt.Color;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

public class HexChat extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgStyle = this.settings.createGroup("LikeSetting");
   private final Setting<HexChat.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Mode")).description("SelectTextColor'sSpawnCompleteMode.")).defaultValue(HexChat.Mode.Quad)).build()
      );
   private final Setting<SettingColor> color = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("SingleColorColor"))
                  .description("SingleColorMode'sColor."))
               .defaultValue(new SettingColor(255, 0, 0, 255))
               .visible(() -> this.mode.get() == HexChat.Mode.Static))
            .build()
      );
   private final Setting<SettingColor> color1 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("Color 1 (Start)"))
                  .description("'sStartColor (GoldColor)."))
               .defaultValue(new SettingColor(255, 215, 0, 255))
               .visible(() -> this.mode.get() == HexChat.Mode.Gradient || this.mode.get() == HexChat.Mode.Quad))
            .build()
      );
   private final Setting<SettingColor> color2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("Color 2"))
                  .description("'sColor (GreenColor)."))
               .defaultValue(new SettingColor(17, 255, 0, 255))
               .visible(() -> this.mode.get() == HexChat.Mode.Gradient || this.mode.get() == HexChat.Mode.Quad))
            .build()
      );
   private final Setting<SettingColor> color3 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("Color 3"))
                  .description("'sColor (BlueColor)."))
               .defaultValue(new SettingColor(0, 191, 255, 255))
               .visible(() -> this.mode.get() == HexChat.Mode.Quad))
            .build()
      );
   private final Setting<SettingColor> color4 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("Color 4 (end)"))
                  .description("'sendColor (PinkColor)."))
               .defaultValue(new SettingColor(255, 105, 180, 255))
               .visible(() -> this.mode.get() == HexChat.Mode.Quad))
            .build()
      );
   private final Setting<Double> rainbowSpread = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("Degree"))
                  .description("Coloratin'sRate. NumberlittleColorslow."))
               .defaultValue(0.1)
               .min(0.01)
               .sliderMax(1.0)
               .visible(() -> this.mode.get() == HexChat.Mode.Rainbow))
            .build()
      );
   private final Setting<HexChat.VanillaColor> vanillaColor = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("Color")).description("Select Minecraft Support'sColor (&0-9 &a-f)."))
                  .defaultValue(HexChat.VanillaColor.WHITE))
               .visible(() -> this.mode.get() == HexChat.Mode.Vanilla))
            .build()
      );
   private final Setting<Boolean> bold = this.sgStyle
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Bold"))
                  .description("Bold (&l)."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> italic = this.sgStyle
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Italic"))
                  .description("Italic (&o)."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> underline = this.sgStyle
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("downLine"))
                  .description("downLine (&n)."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> strikethrough = this.sgStyle
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DeleteLine"))
                  .description("DeleteLine (&m)."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> obfuscated = this.sgStyle
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("RandomText"))
                  .description("RandomText (&k)."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> ignoreCommands = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("PointCommand"))
                  .description("/ OpenHead'sPointCommand (DefenseStopPointCommandEffect)."))
               .defaultValue(true))
            .build()
      );

   public HexChat() {
      super(AddonTemplate.CATEGORY, "HexChat", "Colours your outgoing chat messages with hex colours or a gradient.");
   }

   @EventHandler
   private void onMessageSend(SendMessageEvent event) {
      String message = event.message;
      if (!(Boolean)this.ignoreCommands.get() || !message.startsWith("/")) {
         StringBuilder styleBuilder = new StringBuilder();
         if ((Boolean)this.bold.get()) {
            styleBuilder.append("&l");
         }

         if ((Boolean)this.italic.get()) {
            styleBuilder.append("&o");
         }

         if ((Boolean)this.underline.get()) {
            styleBuilder.append("&n");
         }

         if ((Boolean)this.strikethrough.get()) {
            styleBuilder.append("&m");
         }

         if ((Boolean)this.obfuscated.get()) {
            styleBuilder.append("&k");
         }

         String styleSuffix = styleBuilder.toString();

         event.message = switch ((HexChat.Mode)this.mode.get()) {
            case Static -> this.getHexCode((SettingColor)this.color.get()) + styleSuffix + message;
            case Gradient -> this.applyGradient(message, styleSuffix, false);
            case Quad -> this.applyGradient(message, styleSuffix, true);
            case Rainbow -> this.applyRainbow(message, styleSuffix);
            case Vanilla -> this.applyVanilla(message, styleSuffix);
            default -> message;
         };
      }
   }

   private String applyGradient(String text, String style, boolean isQuad) {
      StringBuilder builder = new StringBuilder();
      int length = text.length();

      for (int i = 0; i < length; i++) {
         char c = text.charAt(i);
         double progress = length <= 1 ? 0.0 : (double)i / (length - 1);
         int r;
         int g;
         int b;
         if (!isQuad) {
            r = this.interpolate(((SettingColor)this.color1.get()).r, ((SettingColor)this.color2.get()).r, progress);
            g = this.interpolate(((SettingColor)this.color1.get()).g, ((SettingColor)this.color2.get()).g, progress);
            b = this.interpolate(((SettingColor)this.color1.get()).b, ((SettingColor)this.color2.get()).b, progress);
         } else if (progress < 0.333) {
            double subProgress = progress * 3.0;
            r = this.interpolate(((SettingColor)this.color1.get()).r, ((SettingColor)this.color2.get()).r, subProgress);
            g = this.interpolate(((SettingColor)this.color1.get()).g, ((SettingColor)this.color2.get()).g, subProgress);
            b = this.interpolate(((SettingColor)this.color1.get()).b, ((SettingColor)this.color2.get()).b, subProgress);
         } else if (progress < 0.666) {
            double subProgress = (progress - 0.333) * 3.0;
            r = this.interpolate(((SettingColor)this.color2.get()).r, ((SettingColor)this.color3.get()).r, subProgress);
            g = this.interpolate(((SettingColor)this.color2.get()).g, ((SettingColor)this.color3.get()).g, subProgress);
            b = this.interpolate(((SettingColor)this.color2.get()).b, ((SettingColor)this.color3.get()).b, subProgress);
         } else {
            double subProgress = (progress - 0.666) * 3.0;
            if (subProgress > 1.0) {
               subProgress = 1.0;
            }

            r = this.interpolate(((SettingColor)this.color3.get()).r, ((SettingColor)this.color4.get()).r, subProgress);
            g = this.interpolate(((SettingColor)this.color3.get()).g, ((SettingColor)this.color4.get()).g, subProgress);
            b = this.interpolate(((SettingColor)this.color3.get()).b, ((SettingColor)this.color4.get()).b, subProgress);
         }

         builder.append(String.format("&#%02X%02X%02X", r, g, b));
         builder.append(style);
         builder.append(c);
      }

      return builder.toString();
   }

   private String applyRainbow(String text, String style) {
      StringBuilder builder = new StringBuilder();
      int length = text.length();
      double spread = (Double)this.rainbowSpread.get();

      for (int i = 0; i < length; i++) {
         char c = text.charAt(i);
         float hue = (float)(i * spread % 1.0);
         int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int r = rgb >> 16 & 0xFF;
         int g = rgb >> 8 & 0xFF;
         int b = rgb & 0xFF;
         builder.append(String.format("&#%02X%02X%02X", r, g, b));
         builder.append(style);
         builder.append(c);
      }

      return builder.toString();
   }

   private String applyVanilla(String text, String style) {
      String colorCode = "&" + ((HexChat.VanillaColor)this.vanillaColor.get()).getCode();
      return colorCode + style + text;
   }

   private int interpolate(int start, int end, double factor) {
      return (int)(start + (end - start) * factor);
   }

   private String getHexCode(SettingColor color) {
      return String.format("&#%02X%02X%02X", color.r, color.g, color.b);
   }

   public static enum Mode {
      Static("SingleColor"),
      Gradient("Color"),
      Quad("Color"),
      Rainbow("Mode"),
      Vanilla("Color");

      private final String title;

      private Mode(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }

   public static enum VanillaColor {
      BLACK("BlackColor", "0"),
      DARK_BLUE("BlueColor", "1"),
      DARK_GREEN("GreenColor", "2"),
      DARK_AQUA("SkyBlueColor", "3"),
      DARK_RED("RedColor", "4"),
      DARK_PURPLE("PurpleColor", "5"),
      GOLD("GoldYellowColor", "6"),
      GRAY("GrayColor", "7"),
      DARK_GRAY("GrayColor", "8"),
      BLUE("PurpleColor", "9"),
      GREEN("GreenColor", "a"),
      AQUA("BlueColor", "b"),
      RED("RedColor", "c"),
      LIGHT_PURPLE("PinkRedColor", "d"),
      YELLOW("YellowColor", "e"),
      WHITE("WhiteColor", "f");

      private final String title;
      private final String code;

      private VanillaColor(String title, String code) {
         this.title = title;
         this.code = code;
      }

      @Override
      public String toString() {
         return this.title;
      }

      public String getCode() {
         return this.code;
      }
   }
}
