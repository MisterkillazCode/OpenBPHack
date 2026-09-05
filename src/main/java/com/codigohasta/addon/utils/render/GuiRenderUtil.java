package com.codigohasta.addon.utils.render;

import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.math.MathHelper;

public class GuiRenderUtil {
   public static int toARGB(int r, int g, int b, int a) {
      return a << 24 | r << 16 | g << 8 | b;
   }

   public static int toARGB(int r, int g, int b, float a) {
      return toARGB(r, g, b, (int)(a * 255.0F));
   }

   public static Color toColor(int argb) {
      return new Color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >> 24 & 0xFF);
   }

   public static void drawRound(HudRenderer r, double x, double y, double w, double h, double radius, Color color) {
      radius = clamp(radius, w, h);
      if (!(w <= 0.0) && !(h <= 0.0) && color.a != 0) {
         r.quad(x + radius, y, w - 2.0 * radius, radius, color);
         r.quad(x, y + radius, w, h - 2.0 * radius, color);
         r.quad(x + radius, y + h - radius, w - 2.0 * radius, radius, color);
         int ri = (int)Math.ceil(radius);

         for (int iy = 0; iy < ri; iy++) {
            double dy = radius - iy - 0.5;
            double dx = Math.sqrt(Math.max(0.0, radius * radius - dy * dy));
            double inset = Math.max(0.0, radius - dx);
            r.quad(x, y + iy, inset, 1.0, color);
            r.quad(x + w - inset, y + iy, inset, 1.0, color);
            r.quad(x, y + h - iy - 1.0, inset, 1.0, color);
            r.quad(x + w - inset, y + h - iy - 1.0, inset, 1.0, color);
         }
      }
   }

   public static void drawRoundGradient(HudRenderer r, double x, double y, double w, double h, double radius, Color cTL, Color cTR, Color cBR, Color cBL) {
      radius = clamp(radius, w, h);
      if (!(w <= 0.0) && !(h <= 0.0)) {
         r.quad(x, y + radius, w, h - 2.0 * radius, cTL, cTR, cBR, cBL);
         r.quad(x + radius, y, w - 2.0 * radius, radius, cTL, cTR, cBR, cBL);
         r.quad(x + radius, y + h - radius, w - 2.0 * radius, radius, cTL, cTR, cBR, cBL);
         int ri = (int)Math.ceil(radius);

         for (int iy = 0; iy < ri; iy++) {
            double dy = radius - iy - 0.5;
            double dx = Math.sqrt(Math.max(0.0, radius * radius - dy * dy));
            double inset = Math.max(0.0, radius - dx);
            r.quad(x, y + iy, inset, 1.0, cTL);
            r.quad(x + w - inset, y + iy, inset, 1.0, cTR);
            r.quad(x, y + h - iy - 1.0, inset, 1.0, cBL);
            r.quad(x + w - inset, y + h - iy - 1.0, inset, 1.0, cBR);
         }
      }
   }

   public static void drawShadow(HudRenderer r, double x, double y, double w, double h, double radius, Color color) {
      int layers = 6;

      for (int i = layers; i > 0; i--) {
         double sp = i * 1.2;
         float a = color.a / 255.0F * (1.0F / (layers * 2) * (layers - i + 1));
         Color c = new Color(color.r, color.g, color.b, (int)(a * 255.0F));
         drawRound(r, x - sp, y - sp, w + sp * 2.0, h + sp * 2.0, radius + sp, c);
      }
   }

   private static double clamp(double r, double w, double h) {
      return MathHelper.clamp(r, 0.0, Math.min(w, h) / 2.0);
   }
}
