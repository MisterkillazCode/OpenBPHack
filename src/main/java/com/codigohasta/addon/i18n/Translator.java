package com.codigohasta.addon.i18n;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;

public class Translator {
   private final JsonObject missing = new JsonObject();
   private Map<String, String> currentLangStrings = null;
   private String loadedLocale = null;
   public static String forcedLocale = "zh_cn";

   public String translate(String key, String def) {
      if (this.currentLangStrings == null) {
         return def;
      } else {
         String v = this.currentLangStrings.get(key);
         if (v != null && !v.isEmpty()) {
            return v;
         } else {
            synchronized (this.missing) {
               this.missing.addProperty(key, def);
               return def;
            }
         }
      }
   }

   public void reload(ResourceManager manager) {
      String locale = this.getCurrentLangCode();
      if (!locale.equals(this.loadedLocale) || this.currentLangStrings == null) {
         this.loadedLocale = locale;
         HashMap<String, String> map = new HashMap<>();
         this.loadTranslations(manager, locale, map::put);
         this.currentLangStrings = Collections.unmodifiableMap(map);
         this.dumpMissing();
      }
   }

   public void setLocale(String locale) {
      forcedLocale = locale;
      this.loadedLocale = null;
   }

   private String getCurrentLangCode() {
      if (forcedLocale != null && !forcedLocale.isEmpty()) {
         return forcedLocale.toLowerCase();
      } else {
         try {
            return MinecraftClient.getInstance().getLanguageManager().getLanguage().toLowerCase();
         } catch (Throwable var2) {
            return "en_us";
         }
      }
   }

   private void loadTranslations(ResourceManager manager, String langCode, BiConsumer<String, String> consumer) {
      Identifier id = Identifier.of("bphack", "lang/" + langCode + ".json");
      Optional<Resource> opt = manager.getResource(id);
      if (opt.isPresent()) {
         try (InputStream s = opt.get().getInputStream()) {
            Language.load(s, consumer);
         } catch (Exception var11) {
            System.out.println("[BPHack-I18n] 加载语言 " + langCode + " 失败: " + var11);
         }
      } else {
         System.out.println("[BPHack-I18n] 未找到语言文件: " + langCode);
      }
   }

   private void dumpMissing() {
      try {
         Gson g = new GsonBuilder().setPrettyPrinting().create();
         Path p = Paths.get("lang_missing.json");

         try (BufferedWriter w = Files.newBufferedWriter(p)) {
            g.toJson(this.missing, w);
         }
      } catch (Exception var8) {
      }
   }
}
