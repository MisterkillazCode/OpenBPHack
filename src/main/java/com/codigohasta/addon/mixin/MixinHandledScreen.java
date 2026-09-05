package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.ShulkerViewer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HandledScreen.class})
public abstract class MixinHandledScreen {
   @Inject(
      method = {"renderMain"},
      at = {@At("TAIL")}
   )
   private void onRenderMain(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (ShulkerViewer.INSTANCE != null) {
         ShulkerViewer.INSTANCE.onScreenRender((Screen)this, context, mouseX, mouseY);
      }
   }

   @Inject(
      method = {"drawMouseoverTooltip"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onDrawMouseoverTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
      if (ShulkerViewer.INSTANCE != null && ShulkerViewer.INSTANCE.isActive()) {
         if (ShulkerViewer.hoveredShulker((Screen)this, x, y) != null) {
            ci.cancel();
         }
      }
   }
}
