package com.codigohasta.addon.mixin;

import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({WContainer.class})
public abstract class MixinWContainerScroll {
   @Inject(
      method = {"mouseScrolled"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void imgOnMouseScrolled(double amount, CallbackInfoReturnable<Boolean> cir) {
      if (this.getClass().getName().contains("WCategoryController")) {
         WContainer self = (WContainer)this;

         for (Cell<?> cell : self.cells) {
            if (cell.widget().mouseScrolled(amount)) {
               cir.setReturnValue(true);
               return;
            }
         }

         double speed = self.theme.scale(24.0);
         self.moveCells(0.0, amount * speed);
         cir.setReturnValue(true);
      }
   }
}
