package com.codigohasta.addon.mixin.i18n;

import java.util.List;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.utils.render.FontUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Fonts.class},
   remap = false
)
public class FontsMixin {
   @Shadow
   @Final
   public static List<FontFamily> FONT_FAMILIES;
   @Shadow
   @Final
   public static String[] BUILTIN_FONTS;

   @Inject(
      method = {"refresh"},
      at = {@At("HEAD")}
   )
   private static void changeDefaultFont(CallbackInfo ci) {
      BUILTIN_FONTS[1] = "WenQuanWeiMiHei";
   }

   @Inject(
      method = {"refresh"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"
      )}
   )
   private static void refresh(CallbackInfo ci) {
      FontUtils.loadBuiltin(FONT_FAMILIES, "WenQuanWeiMiHei");
   }
}
