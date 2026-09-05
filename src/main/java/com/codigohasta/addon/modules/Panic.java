package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class Panic extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> restoreOnDisable = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("restore-on-disable")).description("DisableModuleTimeofbeforeDisable'sFeature")).defaultValue(true))
            .build()
      );
   private final Setting<String> whitelist = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                     .name("whitelist"))
                  .description("WhiteNameSingleModuleName, Use"))
               .defaultValue("ClickGui,HUD,Offhand"))
            .build()
      );
   private final List<Module> disabledModules = new ArrayList<>();

   public Panic() {
      super(AddonTemplate.CATEGORY, "Panic", "Disables every active module at once, keeping a whitelist. Can restore them when turned off.");
   }

   public void onActivate() {
      this.disabledModules.clear();
      String[] whitelistNames = ((String)this.whitelist.get()).split(",");
      List<String> whitelistModules = new ArrayList<>();

      for (String name : whitelistNames) {
         whitelistModules.add(name.trim().toLowerCase());
      }

      for (Module module : Modules.get().getAll()) {
         if (module != this && !this.isWhitelisted(module, whitelistModules) && module.isActive()) {
            this.disabledModules.add(module);
            module.toggle();
         }
      }

      this.info("AlreadyStopStop" + this.disabledModules.size() + "Feature", new Object[0]);
   }

   public void onDeactivate() {
      if ((Boolean)this.restoreOnDisable.get() && !this.disabledModules.isEmpty()) {
         int restoredCount = 0;

         for (Module module : this.disabledModules) {
            if (!module.isActive()) {
               module.toggle();
               restoredCount++;
            }
         }

         this.info("Already" + restoredCount + "Feature", new Object[0]);
      }

      this.disabledModules.clear();
   }

   public String getInfoString() {
      return this.disabledModules.isEmpty() ? "" : String.valueOf(this.disabledModules.size());
   }

   public void panicNow() {
      if (!this.isActive()) {
         this.toggle();
      }
   }

   public void restoreNow() {
      if (this.isActive()) {
         this.toggle();
      }
   }

   private boolean isWhitelisted(Module module, List<String> whitelistModules) {
      if (module == null) {
         return false;
      } else {
         for (String whitelistName : whitelistModules) {
            if (module.name.equalsIgnoreCase(whitelistName)) {
               return true;
            }
         }

         return false;
      }
   }
}
