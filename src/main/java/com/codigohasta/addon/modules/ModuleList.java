package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

public class ModuleList extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> x = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("xPosition")).description("XMark")).defaultValue(1910)).sliderRange(0, 2560).build());
   private final Setting<Integer> y = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("yPosition")).description("YMark")).defaultValue(10)).sliderRange(0, 1440).build());
   private final Setting<Boolean> additionalInfo = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Displayoutside"))
                  .description("DisplayModule'soutside( [Mode])"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> onlyBind = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DisplaySet"))
                  .description("OnlyDisplaySetKey'sModule"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> shadow = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("TextShadow"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> moduleColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("ModuleNameColor"))
            .defaultValue(new SettingColor(210, 245, 255))
            .build()
      );
   private final Setting<SettingColor> activeColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("EnableStatusColor"))
               .description("outsideTextText'sColor"))
            .defaultValue(new SettingColor(0, 255, 170))
            .build()
      );
   private final Setting<SettingColor> background = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("BackgroundColor"))
            .defaultValue(new SettingColor(10, 16, 28, 110))
            .build()
      );
   private final Setting<SettingColor> tagColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("MarkColor"))
               .description("Left'sneonaccentColor"))
            .defaultValue(new SettingColor(0, 220, 255))
            .build()
      );
   private final double slideSpeed = 0.2;
   private final double fadeSpeed = 0.15;
   private final double ySpeed = 0.3;
   private final Map<Module, ModuleList.ModuleEntry> moduleEntries = new HashMap<>();

   public ModuleList() {
      super(AddonTemplate.CATEGORY, "ModuleList", "Draws an on-screen list of your modules (leaveshack style) with configurable position and colours.");
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         TextRenderer textRenderer = TextRenderer.get();
         boolean useShadow = (Boolean)this.shadow.get();

         for (Module m : Modules.get().getAll()) {
            if (!this.moduleEntries.containsKey(m)) {
               this.moduleEntries.put(m, new ModuleList.ModuleEntry(m));
            }
         }

         List<Module> activeModules = Modules.get()
            .getAll()
            .stream()
            .filter(mx -> mx != this && mx.isActive() && (!(Boolean)this.onlyBind.get() || mx.keybind.isSet()))
            .sorted(Comparator.comparingDouble(mx -> -textRenderer.getWidth(this.getDisplayText(mx), useShadow)))
            .collect(Collectors.toList());
         double drawY = ((Integer)this.y.get()).intValue();

         for (Module module : activeModules) {
            ModuleList.ModuleEntry entry = this.moduleEntries.get(module);
            String fullText = this.getDisplayText(module);
            double width = textRenderer.getWidth(fullText, useShadow);
            double height = textRenderer.getHeight();
            double targetX = ((Integer)this.x.get()).intValue() - width;
            entry.x = this.lerp(entry.x, targetX, 0.2);
            entry.y = this.lerp(entry.y, drawY, 0.3);
            entry.fade = this.lerp(entry.fade, 1.0, 0.15);
            if (entry.fade > 0.01) {
               this.renderEntry(event, textRenderer, entry, fullText, width, height, useShadow);
            }

            drawY += height + 6.0;
         }

         for (Module mx : this.moduleEntries.keySet()) {
            if (!activeModules.contains(mx)) {
               ModuleList.ModuleEntry entry = this.moduleEntries.get(mx);
               if (entry.fade <= 0.01 && Math.abs(entry.x - ((Integer)this.x.get() + 50)) < 1.0) {
                  entry.fade = 0.0;
               } else {
                  entry.fade = this.lerp(entry.fade, 0.0, 0.15);
                  entry.x = this.lerp(entry.x, (Integer)this.x.get() + 50, 0.2);
                  if (entry.fade > 0.01) {
                     String fullText = this.getDisplayText(mx);
                     double width = textRenderer.getWidth(fullText, useShadow);
                     double height = textRenderer.getHeight();
                     this.renderEntry(event, textRenderer, entry, fullText, width, height, useShadow);
                  }
               }
            }
         }
      }
   }

   private void renderEntry(
      Render2DEvent event, TextRenderer textRenderer, ModuleList.ModuleEntry entry, String fullText, double width, double height, boolean useShadow
   ) {
      double x = entry.x;
      double y = entry.y;
      double f = entry.fade;
      Renderer2D.COLOR.begin();
      Renderer2D.COLOR.quad(x - 5.0, y - 3.0, width + 10.0, height + 6.0, new SettingColor(0, 220, 255, (int)(45.0 * f)));
      SettingColor bg = (SettingColor)this.background.get();
      Renderer2D.COLOR.quad(x - 4.0, y - 2.0, width + 8.0, height + 4.0, new SettingColor(bg.r, bg.g, bg.b, (int)(bg.a * f)));
      SettingColor ac = (SettingColor)this.tagColor.get();
      Renderer2D.COLOR.quad(x - 4.0, y - 2.0, 2.0, height + 4.0, new SettingColor(ac.r, ac.g, ac.b, (int)(ac.a * f)));
      Renderer2D.COLOR.render();
      SettingColor mainColor = new SettingColor((SettingColor)this.moduleColor.get());
      mainColor.a = (int)(mainColor.a * f);
      textRenderer.render(entry.module.title, x, y, mainColor, useShadow);
      String info = this.getModuleInfo(entry.module);
      if (!info.isEmpty()) {
         double nameWidth = textRenderer.getWidth(entry.module.title, useShadow);
         double spaceWidth = textRenderer.getWidth(" ", useShadow);
         SettingColor infoColor = new SettingColor((SettingColor)this.activeColor.get());
         infoColor.a = (int)(infoColor.a * f);
         textRenderer.render(info, x + nameWidth + spaceWidth, y, infoColor, useShadow);
      }
   }

   private String getDisplayText(Module module) {
      String info = this.getModuleInfo(module);
      return module.title + (info.isEmpty() ? "" : " " + info);
   }

   private String getModuleInfo(Module module) {
      if (!(Boolean)this.additionalInfo.get()) {
         return "";
      } else {
         String info = module.getInfoString();
         return info != null ? info : "";
      }
   }

   private double lerp(double start, double end, double step) {
      return start + (end - start) * step;
   }

   private static class ModuleEntry {
      final Module module;
      double x = 0.0;
      double y = 0.0;
      double fade = 0.0;

      ModuleEntry(Module module) {
         this.module = module;
      }
   }
}
