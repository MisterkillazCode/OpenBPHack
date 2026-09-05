package com.codigohasta.addon.themes;

import java.lang.reflect.Field;
import java.util.List;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme.ThreeStateColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.elements.ActiveModulesHud;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public final class BPHackTheme {
   public static final SettingColor LB_BLUE = c(55, 135, 255);
   public static final SettingColor LB_CYAN = c(0, 210, 255);
   public static final SettingColor LB_DARK = c(10, 14, 22);
   public static final SettingColor LB_PANEL = c(18, 24, 36);
   public static final SettingColor LB_TEXT = c(230, 241, 251);
   public static final SettingColor LB_TEXT2 = c(130, 148, 179);

   private BPHackTheme() {
   }

   private static SettingColor c(int r, int g, int b) {
      return new SettingColor(r, g, b, 255);
   }

   private static SettingColor c(int r, int g, int b, int a) {
      return new SettingColor(r, g, b, a);
   }

   public static void apply(MeteorGuiTheme theme) {
      theme.accentColor.set(LB_BLUE);
      theme.textColor.set(LB_TEXT);
      theme.textSecondaryColor.set(LB_TEXT2);
      theme.textHighlightColor.set(c(55, 135, 255, 120));
      theme.titleTextColor.set(c(255, 255, 255));
      theme.moduleBackground.set(c(55, 135, 255));
      theme.checkboxColor.set(LB_CYAN);
      theme.plusColor.set(c(45, 215, 160));
      theme.minusColor.set(c(255, 90, 120));
      theme.favoriteColor.set(c(255, 220, 70));
      theme.loggedInColor.set(LB_CYAN);
      theme.placeholderColor.set(c(120, 140, 170, 45));
      theme.separatorText.set(c(150, 200, 255));
      theme.separatorCenter.set(c(55, 135, 255, 200));
      theme.separatorEdges.set(c(45, 60, 85, 255));
      theme.sliderLeft.set(c(35, 115, 220));
      theme.sliderRight.set(c(30, 42, 64));
      setThree(theme.backgroundColor, 18, 24, 36, 28, 38, 56, 12, 18, 28);
      setThree(theme.outlineColor, 45, 65, 95, 55, 135, 255, 75, 160, 255);
      setThree(theme.scrollbarColor, 45, 65, 95, 55, 135, 255, 75, 160, 255);
      setThree(theme.sliderHandle, 55, 135, 255, 80, 165, 255, 120, 190, 255);
      patchHud();
   }

   private static void patchHud() {
      Hud hud = Hud.get();
      if (hud != null) {
         try {
            hud.textColors.set(List.of((SettingColor)LB_BLUE.copy(), (SettingColor)LB_TEXT2.copy(), c(45, 215, 160), c(255, 90, 120)));
         } catch (Exception var9) {
         }

         Object flatMode = enumValue("meteordevelopment.meteorclient.systems.hud.elements.ActiveModulesHud$ColorMode", "Flat");
         SettingColor accent = LB_BLUE.toSetting();
         SettingColor secondary = LB_TEXT2.toSetting();

         for (HudElement element : hud) {
            if (element instanceof ActiveModulesHud am) {
               if (flatMode != null) {
                  setSetting(am, "colorMode", flatMode);
               }

               setSetting(am, "flatColor", accent);
               setSetting(am, "moduleInfoColor", secondary);
            } else if (element instanceof TextHud th) {
               String text = (String)th.text.get();
               if (text != null && text.contains("{meteor.name}")) {
                  th.text.set(text.replace("{meteor.name}", "BPHack"));
               }
            }
         }
      }
   }

   private static void setThree(ThreeStateColorSetting setting, int nr, int ng, int nb, int hr, int hg, int hb, int pr, int pg, int pb) {
      try {
         Field fN = ThreeStateColorSetting.class.getDeclaredField("normal");
         Field fH = ThreeStateColorSetting.class.getDeclaredField("hovered");
         Field fP = ThreeStateColorSetting.class.getDeclaredField("pressed");
         fN.setAccessible(true);
         fH.setAccessible(true);
         fP.setAccessible(true);
         ((Setting)fN.get(setting)).set(c(nr, ng, nb));
         ((Setting)fH.get(setting)).set(c(hr, hg, hb));
         ((Setting)fP.get(setting)).set(c(pr, pg, pb));
      } catch (Exception var13) {
      }
   }

   private static void setSetting(Object target, String fieldName, Object value) {
      try {
         Field field = target.getClass().getDeclaredField(fieldName);
         field.setAccessible(true);
         Setting setting = (Setting)field.get(target);
         if (setting != null) {
            setting.set(value);
         }
      } catch (Exception var5) {
      }
   }

   private static Object enumValue(String className, String constant) {
      try {
         Class<Enum> clazz = (Class<Enum>)Class.forName(className);
         return Enum.valueOf(clazz, constant);
      } catch (Exception var3) {
         return null;
      }
   }
}
