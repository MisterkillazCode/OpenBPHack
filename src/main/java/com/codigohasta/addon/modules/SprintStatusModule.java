package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

public class SprintStatusModule extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgDisplay = this.settings.createGroup("insideTolerateSetting");
   private final SettingGroup sgColors = this.settings.createGroup("ColorSetting");
   private final Setting<Double> scale = this.sgGeneral.add(((Builder)new Builder().name("TextTextSize")).defaultValue(1.5).min(0.5).sliderMax(5.0).build());
   private final Setting<Double> posX = this.sgGeneral.add(((Builder)new Builder().name("Position-X")).defaultValue(500.0).min(0.0).sliderMax(2500.0).build());
   private final Setting<Double> posY = this.sgGeneral.add(((Builder)new Builder().name("Position-Y")).defaultValue(230.0).min(0.0).sliderMax(2500.0).build());
   private final Setting<Boolean> useChinese = this.sgDisplay
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("UseinText"))
                  .description("EnableDisplay: run, DisableDisplay: Sprint"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> showIcon = this.sgDisplay
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DisplayImageMark"))
                  .description("Display ▶▶ ■"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> sprintColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("DashStatusColor"))
            .defaultValue(new SettingColor(0, 255, 0))
            .build()
      );
   private final Setting<SettingColor> idleColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("DashStatusColor"))
            .defaultValue(new SettingColor(255, 0, 0))
            .build()
      );
   private final Setting<Boolean> background = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("DisplayBackground"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> bgColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("BackgroundColor"))
               .defaultValue(new SettingColor(0, 0, 0, 150))
               .visible(this.background::get))
            .build()
      );

   public SprintStatusModule() {
      super(AddonTemplate.CATEGORY, "SprintStatusModule", "Displays your current sprint status on screen.");
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (this.mc.player != null) {
         boolean isSprinting = this.mc.player.isSprinting();
         String icon = "";
         if ((Boolean)this.showIcon.get()) {
            icon = isSprinting ? "▶▶ " : "■ ";
         }

         String statusText = "";
         if ((Boolean)this.useChinese.get()) {
            statusText = isSprinting ? "run: Enable" : "run: Disable";
         } else {
            statusText = isSprinting ? "Sprint: ON" : "Sprint: OFF";
         }

         String finalText = icon + statusText;
         SettingColor textColor = isSprinting ? (SettingColor)this.sprintColor.get() : (SettingColor)this.idleColor.get();
         TextRenderer renderer = TextRenderer.get();
         double w = renderer.getWidth(finalText) * (Double)this.scale.get();
         double h = renderer.getHeight() * (Double)this.scale.get();
         double x = (Double)this.posX.get();
         double y = (Double)this.posY.get();
         if ((Boolean)this.background.get()) {
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(x - 4.0, y - 4.0, w + 8.0, h + 8.0, (Color)this.bgColor.get());
            Renderer2D.COLOR.render();
         }

         renderer.begin((Double)this.scale.get(), false, true);
         renderer.render(finalText, x, y, textColor, true);
         renderer.end();
      }
   }
}
