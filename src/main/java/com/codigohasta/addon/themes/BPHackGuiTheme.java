package com.codigohasta.addon.themes;

import java.lang.reflect.Field;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorAccount;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorHorizontalSeparator;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorLabel;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorModule;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorMultiLabel;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorQuad;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorSection;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorTooltip;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorTopBar;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorVerticalSeparator;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorView;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorWindow;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorDropdown;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorSlider;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorTextBox;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorButton;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorCheckbox;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorConfirmedButton;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorConfirmedMinus;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorFavorite;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorMinus;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorPlus;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorTriangle;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.WAccount;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WQuad;
import meteordevelopment.meteorclient.gui.widgets.WTooltip;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;
import meteordevelopment.meteorclient.gui.widgets.WVerticalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection.WHeader;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox.Renderer;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WFavorite;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WTriangle;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import sun.misc.Unsafe;

public class BPHackGuiTheme extends MeteorGuiTheme {
   public static final Color LB_BLUE = new Color(55, 135, 255);
   public static final Color LB_CYAN = new Color(0, 210, 255);
   public static final Color LB_DARK = new Color(10, 14, 22);
   public static final Color LB_PANEL = new Color(18, 24, 36);
   public static final Color LB_ACCENT = new Color(55, 135, 255);
   public static final Color NEON_CYAN = LB_CYAN;
   public static final Color NEON_PURPLE = LB_BLUE;
   public static final Color NEON_PINK = new Color(255, 90, 120);

   public BPHackGuiTheme() {
      setName(this, "BPHack");
      BPHackTheme.apply(this);
   }

   private static void setName(Object theme, String name) {
      try {
         Field f = GuiTheme.class.getDeclaredField("name");
         f.setAccessible(true);
         Unsafe unsafe = getUnsafe();
         if (unsafe != null) {
            unsafe.putObject(theme, unsafe.objectFieldOffset(f), name);
         }
      } catch (Exception var4) {
      }
   }

   private static Unsafe getUnsafe() {
      try {
         Field f = Unsafe.class.getDeclaredField("theUnsafe");
         f.setAccessible(true);
         return (Unsafe)f.get(null);
      } catch (Exception var1) {
         return null;
      }
   }

   private static void glass(GuiRenderer r, double x, double y, double w, double h, MeteorGuiTheme theme, boolean pressed, boolean over) {
      Color fill = theme.backgroundColor.get(pressed, over);
      Color border = theme.outlineColor.get(pressed, over);
      double s = theme.scale(1.0);
      r.quad(x, y, w, h, fill);
      r.quad(x, y, w, s, border);
      r.quad(x, y + h - s, w, s, border);
      r.quad(x, y, s, h, border);
      r.quad(x + w - s, y, s, h, border);
   }

   private static double clamp(double v, double a, double b) {
      return Math.max(a, Math.min(b, v));
   }

   public WWindow window(WWidget icon, String title) {
      return (WWindow)this.w(new BPHackGuiTheme.WImgWindow(icon, title));
   }

   public WLabel label(String text, boolean title, double maxWidth) {
      return maxWidth == 0.0 && !text.contains("\n")
         ? (WLabel)this.w(new BPHackGuiTheme.WImgLabel(text, title))
         : (WLabel)this.w(new BPHackGuiTheme.WImgMultiLabel(text, title, maxWidth));
   }

   public WHorizontalSeparator horizontalSeparator(String text) {
      return (WHorizontalSeparator)this.w(new BPHackGuiTheme.WImgHorizontalSeparator(text));
   }

   public WVerticalSeparator verticalSeparator() {
      return (WVerticalSeparator)this.w(new BPHackGuiTheme.WImgVerticalSeparator());
   }

   protected WButton button(String text, GuiTexture texture) {
      return (WButton)this.w(new BPHackGuiTheme.WImgButton(text, texture));
   }

   protected WConfirmedButton confirmedButton(String text, String confirmText, GuiTexture texture) {
      return (WConfirmedButton)this.w(new BPHackGuiTheme.WImgConfirmedButton(text, confirmText, texture));
   }

   public WMinus minus() {
      return (WMinus)this.w(new BPHackGuiTheme.WImgMinus());
   }

   public WConfirmedMinus confirmedMinus() {
      return (WConfirmedMinus)this.w(new BPHackGuiTheme.WImgConfirmedMinus());
   }

   public WPlus plus() {
      return (WPlus)this.w(new BPHackGuiTheme.WImgPlus());
   }

   public WCheckbox checkbox(boolean checked) {
      return (WCheckbox)this.w(new BPHackGuiTheme.WImgCheckbox(checked));
   }

   public WSlider slider(double value, double min, double max) {
      return (WSlider)this.w(new BPHackGuiTheme.WImgSlider(value, min, max));
   }

   public WTextBox textBox(String text, String placeholder, CharFilter filter, Class<? extends Renderer> renderer) {
      return (WTextBox)this.w(new BPHackGuiTheme.WImgTextBox(text, placeholder, filter, renderer));
   }

   public <T> WDropdown<T> dropdown(T[] values, T value) {
      return (WDropdown<T>)this.w(new BPHackGuiTheme.WImgDropdown(values, value));
   }

   public WTriangle triangle() {
      return (WTriangle)this.w(new BPHackGuiTheme.WImgTriangle());
   }

   public WTooltip tooltip(String text) {
      return (WTooltip)this.w(new BPHackGuiTheme.WImgTooltip(text));
   }

   public WView view() {
      return (WView)this.w(new BPHackGuiTheme.WImgView());
   }

   public WSection section(String title, boolean expanded, WWidget headerWidget) {
      return (WSection)this.w(new BPHackGuiTheme.WImgSection(title, expanded, headerWidget));
   }

   public WAccount account(WidgetScreen screen, Account<?> account) {
      return (WAccount)this.w(new BPHackGuiTheme.WImgAccount(screen, account));
   }

   public WWidget module(Module module, String title) {
      return this.w(new BPHackGuiTheme.WImgModule(module, title));
   }

   public WQuad quad(Color color) {
      return (WQuad)this.w(new BPHackGuiTheme.WImgQuad(color));
   }

   public WTopBar topBar() {
      return (WTopBar)this.w(new BPHackGuiTheme.WImgTopBar());
   }

   public WFavorite favorite(boolean checked) {
      return (WFavorite)this.w(new BPHackGuiTheme.WImgFavorite(checked));
   }

   public static class WImgAccount extends WMeteorAccount {
      public WImgAccount(WidgetScreen screen, Account<?> account) {
         super(screen, account);
      }
   }

   public static class WImgButton extends WMeteorButton {
      public WImgButton(String text, GuiTexture texture) {
         super(text, texture);
      }

      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         MeteorGuiTheme theme = this.theme();
         double pad = this.pad();
         BPHackGuiTheme.glass(renderer, this.x, this.y, this.width, this.height, theme, this.pressed, this.mouseOver);
         if (this.text != null) {
            renderer.text(this.text, this.x + this.width / 2.0 - theme.textWidth(this.text) / 2.0, this.y + pad, (Color)theme.textColor.get(), false);
         } else {
            double ts = theme.textHeight();
            renderer.quad(this.x + this.width / 2.0 - ts / 2.0, this.y + pad, ts, ts, this.texture, (Color)theme.textColor.get());
         }
      }
   }

   public static class WImgCheckbox extends WMeteorCheckbox {
      private double animProgress;

      public WImgCheckbox(boolean checked) {
         super(checked);
         this.animProgress = checked ? 1.0 : 0.0;
      }

      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         MeteorGuiTheme theme = this.theme();
         this.animProgress = this.animProgress + (this.checked ? 1 : -1) * delta * 14.0;
         this.animProgress = BPHackGuiTheme.clamp(this.animProgress, 0.0, 1.0);
         BPHackGuiTheme.glass(renderer, this.x, this.y, this.width, this.height, theme, this.pressed, this.mouseOver);
         if (this.animProgress > 0.0) {
            double cs = (this.width - theme.scale(2.0)) / 1.75 * this.animProgress;
            renderer.quad(this.x + (this.width - cs) / 2.0, this.y + (this.height - cs) / 2.0, cs, cs, (Color)theme.checkboxColor.get());
         }
      }
   }

   public static class WImgConfirmedButton extends WMeteorConfirmedButton {
      public WImgConfirmedButton(String text, String confirmText, GuiTexture texture) {
         super(text, confirmText, texture);
      }
   }

   public static class WImgConfirmedMinus extends WMeteorConfirmedMinus {
   }

   public static class WImgDropdown<T> extends WMeteorDropdown<T> {
      public WImgDropdown(T[] values, T value) {
         super(values, value);
      }

      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         MeteorGuiTheme theme = this.theme();
         double pad = this.pad();
         double s = theme.textHeight();
         BPHackGuiTheme.glass(renderer, this.x, this.y, this.width, this.height, theme, this.pressed, this.mouseOver);
         String text = this.get().toString();
         double w = theme.textWidth(text);
         renderer.text(text, this.x + pad + this.maxValueWidth / 2.0 - w / 2.0, this.y + pad, (Color)theme.textColor.get(), false);
         renderer.rotatedQuad(this.x + pad + this.maxValueWidth + pad, this.y + pad, s, s, 0.0, GuiRenderer.TRIANGLE, (Color)theme.textColor.get());
      }
   }

   public static class WImgFavorite extends WMeteorFavorite {
      public WImgFavorite(boolean checked) {
         super(checked);
      }
   }

   public static class WImgHorizontalSeparator extends WMeteorHorizontalSeparator {
      public WImgHorizontalSeparator(String text) {
         super(text);
      }

      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         if (this.text == null) {
            this.renderWithoutText(renderer);
         } else {
            this.renderWithText(renderer);
         }
      }

      private void renderWithoutText(GuiRenderer renderer) {
         MeteorGuiTheme theme = this.theme();
         double s = theme.scale(1.0);
         double w = this.width / 2.0;
         Color c = (Color)theme.separatorEdges.get();
         Color cC = (Color)theme.separatorCenter.get();
         renderer.quad(this.x, this.y + s, w, s, c, cC);
         renderer.quad(this.x + w, this.y + s, w, s, cC, c);
      }

      private void renderWithText(GuiRenderer renderer) {
         MeteorGuiTheme theme = this.theme();
         double s = theme.scale(2.0);
         double h = theme.scale(1.0);
         double textStart = Math.round(this.width / 2.0 - this.textWidth / 2.0 - s);
         double textEnd = s + textStart + this.textWidth + s;
         double offsetY = Math.round(this.height / 2.0);
         Color c = (Color)theme.separatorEdges.get();
         Color cC = (Color)theme.separatorCenter.get();
         renderer.quad(this.x, this.y + offsetY, textStart, h, c, cC);
         renderer.text(this.text, this.x + textStart + s, this.y, (Color)theme.separatorText.get(), false);
         renderer.quad(this.x + textEnd, this.y + offsetY, this.width - textEnd, h, cC, c);
      }
   }

   public static class WImgLabel extends WMeteorLabel {
      public WImgLabel(String text, boolean title) {
         super(text, title);
      }

      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         if (!this.text.isEmpty()) {
            renderer.text(
               this.text,
               this.x,
               this.y,
               this.color != null ? this.color : (this.title ? (Color)this.theme().titleTextColor.get() : (Color)this.theme().textColor.get()),
               this.title
            );
         }
      }
   }

   public static class WImgMinus extends WMeteorMinus {
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         MeteorGuiTheme theme = this.theme();
         double pad = this.pad();
         double s = theme.scale(3.0);
         BPHackGuiTheme.glass(renderer, this.x, this.y, this.width, this.height, theme, this.pressed, this.mouseOver);
         renderer.quad(this.x + pad, this.y + this.height / 2.0 - s / 2.0, this.width - pad * 2.0, s, (Color)theme.minusColor.get());
      }
   }

   public static class WImgModule extends WMeteorModule {
      private final Module mod;
      private final String ttl;
      private double ap1;
      private double ap2;

      public WImgModule(Module module, String title) {
         super(module, title);
         this.mod = module;
         this.ttl = title;
      }

      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         MeteorGuiTheme theme = this.theme();
         double pad = this.pad();
         this.ap1 = this.ap1 + delta * 4.0 * (!this.mod.isActive() && !this.mouseOver ? -1 : 1);
         this.ap1 = BPHackGuiTheme.clamp(this.ap1, 0.0, 1.0);
         this.ap2 = this.ap2 + delta * 6.0 * (this.mod.isActive() ? 1 : -1);
         this.ap2 = BPHackGuiTheme.clamp(this.ap2, 0.0, 1.0);
         BPHackGuiTheme.glass(renderer, this.x, this.y, this.width, this.height, theme, this.pressed, this.mouseOver);
         if (this.ap1 > 0.0) {
            Color fill = (Color)theme.moduleBackground.get();
            renderer.quad(this.x, this.y, this.width * this.ap1, this.height, new Color(fill.r, fill.g, fill.b, (int)(fill.a * this.ap1)));
         }

         if (this.ap2 > 0.0) {
            double bw = theme.scale(2.0);
            renderer.quad(this.x, this.y + this.height * (1.0 - this.ap2), bw, this.height * this.ap2, (Color)theme.accentColor.get());
         }

         double tx = this.x + pad;
         double w = this.width - pad * 2.0;
         if (theme.moduleAlignment.get() == AlignmentX.Center) {
            tx += w / 2.0 - theme.textWidth(this.ttl) / 2.0;
         } else if (theme.moduleAlignment.get() == AlignmentX.Right) {
            tx += w - theme.textWidth(this.ttl);
         }

         renderer.text(this.ttl, tx, this.y + pad, (Color)theme.textColor.get(), false);
      }
   }

   public static class WImgMultiLabel extends WMeteorMultiLabel {
      public WImgMultiLabel(String text, boolean title, double maxWidth) {
         super(text, title, maxWidth);
      }
   }

   public static class WImgPlus extends WMeteorPlus {
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         MeteorGuiTheme theme = this.theme();
         double pad = this.pad();
         double s = theme.scale(3.0);
         BPHackGuiTheme.glass(renderer, this.x, this.y, this.width, this.height, theme, this.pressed, this.mouseOver);
         renderer.quad(this.x + pad, this.y + this.height / 2.0 - s / 2.0, this.width - pad * 2.0, s, (Color)theme.plusColor.get());
         renderer.quad(this.x + this.width / 2.0 - s / 2.0, this.y + pad, s, this.height - pad * 2.0, (Color)theme.plusColor.get());
      }
   }

   public static class WImgQuad extends WMeteorQuad {
      public WImgQuad(Color color) {
         super(color);
      }
   }

   public static class WImgSection extends WMeteorSection {
      public WImgSection(String title, boolean expanded, WWidget headerWidget) {
         super(title, expanded, headerWidget);
      }

      protected WHeader createHeader() {
         return new BPHackGuiTheme.WImgSection.WImgHeader(this.title);
      }

      public class WImgHeader extends WHeader {
         public WImgHeader(String title) {
            super(WImgSection.this, title);
         }

         public void init() {
            this.add(this.theme.horizontalSeparator(this.title)).expandX();
            if (WImgSection.this.headerWidget != null) {
               this.add(WImgSection.this.headerWidget);
            }

            WTriangle t = this.theme.triangle();
            t.theme = this.theme;
            t.action = () -> this.onClick();
            this.add(t).pad(4.0).right().centerY();
         }

         protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            MeteorGuiTheme mtheme = (MeteorGuiTheme)this.theme;
            double s = mtheme.scale(2.0);
            renderer.quad(this.x, this.y + this.height - s, this.width, s, (Color)mtheme.accentColor.get());
         }
      }
   }

   public static class WImgSlider extends WMeteorSlider {
      public WImgSlider(double value, double min, double max) {
         super(value, min, max);
      }

      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         double valueWidth = this.valueWidth();
         MeteorGuiTheme theme = this.theme();
         double s = theme.scale(3.0);
         double handleSize = this.handleSize();
         double bx = this.x + handleSize / 2.0;
         double by = this.y + this.height / 2.0 - s / 2.0;
         renderer.quad(bx, by, this.width - handleSize, s, (Color)theme.sliderRight.get());
         renderer.quad(bx, by, valueWidth, s, (Color)theme.sliderLeft.get());
         renderer.quad(this.x + valueWidth, this.y, handleSize, handleSize, GuiRenderer.CIRCLE, theme.sliderHandle.get(this.dragging, this.handleMouseOver));
      }
   }

   public static class WImgTextBox extends WMeteorTextBox {
      public WImgTextBox(String text, String placeholder, CharFilter filter, Class<? extends Renderer> renderer) {
         super(text, placeholder, filter, renderer);
      }

      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         super.onRender(renderer, mouseX, mouseY, delta);
         MeteorGuiTheme theme = this.theme();
         double s = theme.scale(1.0);
         Color border = theme.outlineColor.get(this.focused, this.mouseOver);
         renderer.quad(this.x, this.y, this.width, s, border);
         renderer.quad(this.x, this.y + this.height - s, this.width, s, border);
         renderer.quad(this.x, this.y, s, this.height, border);
         renderer.quad(this.x + this.width - s, this.y, s, this.height, border);
      }
   }

   public static class WImgTooltip extends WMeteorTooltip {
      public WImgTooltip(String text) {
         super(text);
      }

      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         BPHackGuiTheme.glass(renderer, this.x, this.y, this.width, this.height, this.theme(), false, false);
      }
   }

   public static class WImgTopBar extends WMeteorTopBar {
   }

   public static class WImgTriangle extends WMeteorTriangle {
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         renderer.rotatedQuad(this.x, this.y, this.width, this.height, this.rotation, GuiRenderer.TRIANGLE, (Color)this.theme().accentColor.get());
      }
   }

   public static class WImgVerticalSeparator extends WMeteorVerticalSeparator {
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         MeteorGuiTheme theme = this.theme();
         Color cE = (Color)theme.separatorEdges.get();
         Color cC = (Color)theme.separatorCenter.get();
         double s = theme.scale(1.0);
         double offsetX = Math.round(this.width / 2.0);
         renderer.quad(this.x + offsetX, this.y, s, this.height / 2.0, cE, cE, cC, cC);
         renderer.quad(this.x + offsetX, this.y + this.height / 2.0, s, this.height / 2.0, cC, cC, cE, cE);
      }
   }

   public static class WImgView extends WMeteorView {
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         if (this.canScroll && this.hasScrollBar) {
            double s = this.theme.scale(1.0);
            Color bar = this.theme().scrollbarColor.get(this.focused, this.handleMouseOver);
            Color glow = new Color(bar.r, bar.g, bar.b, Math.min(255, bar.a + 60));
            renderer.quad(this.handleX() - s, this.handleY() - s, this.handleWidth() + s * 2.0, this.handleHeight() + s * 2.0, glow);
            renderer.quad(this.handleX(), this.handleY(), this.handleWidth(), this.handleHeight(), bar);
         }
      }
   }

   public static class WImgWindow extends WMeteorWindow {
      public WImgWindow(WWidget icon, String title) {
         super(icon, title);
      }

      protected meteordevelopment.meteorclient.gui.widgets.containers.WWindow.WHeader header(WWidget icon) {
         return new BPHackGuiTheme.WImgWindow.WImgHeader(icon);
      }

      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         if (this.expanded || this.animProgress > 0.0) {
            BPHackGuiTheme.glass(
               renderer, this.x, this.y + this.header.height, this.width, this.height - this.header.height, this.theme(), false, this.mouseOver
            );
         }
      }

      public class WImgHeader extends meteordevelopment.meteorclient.gui.widgets.containers.WWindow.WHeader {
         public WImgHeader(WWidget icon) {
            super(WImgWindow.this, icon);
         }

         public void init() {
            super.init();
         }

         protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double h = this.height;
            renderer.quad(this.x, this.y, this.width, h, BPHackGuiTheme.LB_BLUE, BPHackGuiTheme.LB_ACCENT, BPHackGuiTheme.LB_ACCENT, BPHackGuiTheme.LB_BLUE);
            renderer.quad(this.x, this.y, this.width, WImgWindow.this.theme().scale(2.0), new Color(255, 255, 255, 40));
         }
      }
   }
}
