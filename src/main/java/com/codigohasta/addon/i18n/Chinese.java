package com.codigohasta.addon.i18n;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.MinecraftClient;

public class Chinese extends Module {
   private final SettingGroup sg = this.settings.getDefaultGroup();
   private final Setting<Chinese.LangMode> language = this.sg
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("language")).description("选择界面显示语言。默认中文；跟随游戏则使用 MC 当前语言。"))
                  .defaultValue(Chinese.LangMode.中文))
               .onChanged(mode -> {
                  I18n.TRANSLATOR.setLocale(mode == Chinese.LangMode.中文 ? "zh_cn" : null);
                  I18n.TRANSLATOR.reload(MinecraftClient.getInstance().getResourceManager());
                  I18nRegistry.retranslateAll();
               }))
            .build()
      );

   public Chinese() {
      super(AddonTemplate.CATEGORY, "中文", "将所有英文界面汉化为中文，包括本功能自身。");
   }

   public static enum LangMode {
      中文,
      跟随游戏;
   }
}
