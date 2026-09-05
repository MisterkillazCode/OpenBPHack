package com.codigohasta.addon.mixin.i18n;

import com.codigohasta.addon.i18n.I18n;
import com.codigohasta.addon.i18n.I18nRegistry;
import com.codigohasta.addon.i18n.ITranslatable;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Module.class},
   priority = 1000
)
public abstract class ModuleMixin implements ITranslatable {
   @Shadow
   @Final
   public String name;
   @Shadow
   @Mutable
   @Final
   public String title;
   @Shadow
   @Mutable
   @Final
   public String description;
   @Shadow
   @Final
   public MeteorAddon addon;
   @Unique
   private String bphackOrigTitle;
   @Unique
   private String bphackOrigDescription;

   @Inject(
      method = {"<init>*"},
      at = {@At("RETURN")}
   )
   public void onInit(CallbackInfo ci) {
      I18n.TRANSLATOR.reload(MinecraftClient.getInstance().getResourceManager());
      this.bphackOrigTitle = this.title;
      this.bphackOrigDescription = this.description;
      I18nRegistry.register(this);
      this.retranslateI18n();
   }

   @Override
   public void retranslateI18n() {
      String pkg = pkgOf(this.addon);
      String origTitle = this.bphackOrigTitle != null ? this.bphackOrigTitle : this.name;
      String origDesc = this.bphackOrigDescription != null ? this.bphackOrigDescription : this.description;
      this.title = I18n.TRANSLATOR.translate("Module." + pkg + "." + this.name, origTitle);
      this.description = I18n.TRANSLATOR.translate("Module." + pkg + "." + this.name + ".Description", origDesc);
   }

   private static String pkgOf(MeteorAddon a) {
      String p = (a != null ? a.name : "Unknown").replace(" ", "-");
      if ("Meteor-Client".equals(p)) {
         p = "Meteor";
      }

      return p;
   }
}
