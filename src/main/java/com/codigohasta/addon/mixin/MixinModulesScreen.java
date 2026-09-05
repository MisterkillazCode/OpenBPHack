package com.codigohasta.addon.mixin;

import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.utils.Utils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(
   targets = {"meteordevelopment.meteorclient.gui.screens.ModulesScreen$WCategoryController"},
   remap = false
)
public abstract class MixinModulesScreen extends WContainer {
   @Overwrite
   protected void onCalculateWidgetPositions() {
      double pad = this.theme.scale(4.0);
      double startX = this.x + pad;
      double curX = startX;
      double curY = this.y;
      double rowHeight = 0.0;
      double windowWidth = Utils.getWindowWidth();

      for (Cell<?> cell : this.cells) {
         double cellWidth = cell.widget().width;
         double cellHeight = cell.widget().height;
         if (curX + cellWidth > windowWidth) {
            curX = startX;
            curY += rowHeight + pad;
            rowHeight = 0.0;
         }

         if (curX > windowWidth) {
            curX = windowWidth / 2.0 - cellWidth / 2.0;
            if (curX < 0.0) {
               curX = 0.0;
            }
         }

         if (curY > Utils.getWindowHeight()) {
            curY = Utils.getWindowHeight() / 2.0 - cellHeight / 2.0;
            if (curY < 0.0) {
               curY = 0.0;
            }
         }

         cell.x = curX;
         cell.y = curY;
         cell.width = cellWidth;
         cell.height = cellHeight;
         cell.alignWidget();
         curX += cellWidth + pad;
         rowHeight = Math.max(rowHeight, cellHeight);
      }
   }
}
