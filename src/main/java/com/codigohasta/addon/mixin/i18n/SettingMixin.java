package com.codigohasta.addon.mixin.i18n;

import com.codigohasta.addon.i18n.I18n;
import com.codigohasta.addon.i18n.I18nRegistry;
import com.codigohasta.addon.i18n.ITranslatable;
import java.util.function.Consumer;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Setting.class})
public class SettingMixin implements ITranslatable {
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
   @Unique
   private String bphackOrigTitle;
   @Unique
   private String bphackOrigDescription;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   public void init(String name, String description, Object defaultValue, Consumer onChanged, Consumer onModuleActivated, IVisible visible, CallbackInfo ci) {
      I18n.TRANSLATOR.reload(MinecraftClient.getInstance().getResourceManager());
      this.bphackOrigTitle = this.title;
      this.bphackOrigDescription = this.description;
      I18nRegistry.register(this);
      this.retranslateI18n();
   }

   @Override
   public void retranslateI18n() {
      String pkg = pkgOf(addonOf(this.getClass().getName()));
      String origTitle = this.bphackOrigTitle != null ? this.bphackOrigTitle : this.name;
      String origDesc = this.bphackOrigDescription != null ? this.bphackOrigDescription : this.description;
      this.title = I18n.TRANSLATOR.translate("Setting." + pkg + "." + this.name, origTitle);
      this.description = I18n.TRANSLATOR.translate("Setting." + pkg + "." + this.name + ".Description", origDesc);
   }

   private static MeteorAddon addonOf(String className) {
      for (MeteorAddon a : AddonManager.ADDONS) {
         if (className.startsWith(a.getPackage())) {
            return a;
         }
      }

      return null;
   }

   private static String pkgOf(MeteorAddon a) {
      String p = (a != null ? a.name : "Unknown").replace(" ", "-");
      if ("Meteor-Client".equals(p)) {
         p = "Meteor";
      }

      return p;
   }
}
