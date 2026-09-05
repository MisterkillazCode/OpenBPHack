package com.codigohasta.addon.mixin;

import com.codigohasta.addon.themes.BPHackGuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {GuiThemes.class},
   remap = false
)
public abstract class MixinGuiThemes {
   @Inject(
      method = {"postInit"},
      at = {@At("TAIL")}
   )
   private static void onPostInit(CallbackInfo ci) {
      try {
         GuiThemes.add(new BPHackGuiTheme());
      } catch (Exception var2) {
      }
   }
}
