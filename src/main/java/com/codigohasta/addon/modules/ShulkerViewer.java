package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.HandledScreenSlotAccessor;
import java.util.List;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.mixin.HandledScreenAccessor;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

public class ShulkerViewer extends Module {
   public static ShulkerViewer INSTANCE;
   private final SettingGroup sg = this.settings.getDefaultGroup();
   private final Setting<Double> scaleSet = this.sg
      .add(((Builder)((Builder)new Builder().name("PopupScale")).description("预览面板缩放比例。")).defaultValue(1.0).min(0.5).max(2.0).sliderRange(0.5, 2.0).build());
   private final Setting<SettingColor> bg = this.sg
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Background"))
               .description("预览面板背景颜色。"))
            .defaultValue(new SettingColor(18, 24, 36, 235))
            .build()
      );

   public ShulkerViewer() {
      super(
         AddonTemplate.SC_CATEGORY,
         "ShulkerViewer",
         "Shulker viewer: hovering a shulker box shows a Chest-style UI of its contents with stack counts. Pure client-side read, sends no packets, fully safe against Grim V1/V2/V3."
      );
      INSTANCE = this;
   }

   public static boolean isShulker(ItemStack stack) {
      return stack != null && !stack.isEmpty() ? stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock : false;
   }

   public static ItemStack hoveredShulker(Screen screen, int mouseX, int mouseY) {
      if (screen instanceof HandledScreenAccessor acc) {
         Slot slot = acc.meteor$getFocusedSlot();
         if (slot == null && screen instanceof HandledScreenSlotAccessor sa) {
            try {
               slot = sa.bphack$getSlotAt(mouseX, mouseY);
            } catch (Throwable var7) {
               slot = null;
            }
         }

         if (slot == null) {
            return null;
         } else {
            ItemStack stack = slot.getStack();
            return isShulker(stack) ? stack : null;
         }
      } else {
         return null;
      }
   }

   private ItemStack[] parseShulker(ItemStack stack) {
      ItemStack[] items = new ItemStack[27];
      ContainerComponent cont = (ContainerComponent)stack.get(DataComponentTypes.CONTAINER);
      if (cont != null) {
         List<ItemStack> all = cont.stream().collect(Collectors.toList());

         for (int i = 0; i < 27 && i < all.size(); i++) {
            items[i] = all.get(i);
         }
      }

      return items;
   }

   public void onScreenRender(Screen screen, DrawContext dc, int mouseX, int mouseY) {
      if (this.isActive()) {
         ItemStack stack = hoveredShulker(screen, mouseX, mouseY);
         if (stack != null) {
            ItemStack[] items = this.parseShulker(stack);
            double sc = (Double)this.scaleSet.get();
            int cell = (int)(18.0 * sc);
            int gap = (int)(2.0 * sc);
            int cols = 9;
            int rows = 3;
            int pad = (int)(6.0 * sc);
            int titleH = (int)(16.0 * sc);
            int innerW = cols * cell + (cols - 1) * gap;
            int innerH = rows * cell + (rows - 1) * gap;
            int pw = innerW + pad * 2;
            int ph = innerH + pad * 2 + titleH;
            int px = mouseX + 18;
            int py = mouseY + 18;
            if (px + pw > this.mc.getWindow().getScaledWidth()) {
               px = mouseX - pw - 18;
            }

            if (py + ph > this.mc.getWindow().getScaledHeight()) {
               py = mouseY - ph - 18;
            }

            if (px < 2) {
               px = 2;
            }

            if (py < 2) {
               py = 2;
            }

            dc.fill(px, py, px + pw, py + ph, ((SettingColor)this.bg.get()).getPacked());
            dc.drawText(this.mc.textRenderer, Text.literal("ShulkerViewer"), px + pad, py + (pad >> 1), 3669936, false);
            int startX = px + pad;
            int startY = py + pad + titleH;

            for (int i = 0; i < 27; i++) {
               int cxi = i % cols;
               int cyi = i / cols;
               int cx = startX + cxi * (cell + gap);
               int cy = startY + cyi * (cell + gap);
               dc.fill(cx, cy, cx + cell, cy + cell, 1426063360);
               ItemStack it = items[i];
               if (it != null && !it.isEmpty()) {
                  RenderUtils.drawItem(dc, it, cx + 1, cy + 1, (float)sc, true, it.getCount() > 1 ? String.valueOf(it.getCount()) : null, false);
               }
            }
         }
      }
   }
}
