package com.codigohasta.addon.i18n;

import java.util.ArrayList;
import java.util.List;

public class I18nRegistry {
   public static final List<ITranslatable> ENTRIES = new ArrayList<>();

   public static void register(ITranslatable entry) {
      if (entry != null) {
         ENTRIES.add(entry);
      }
   }

   public static void retranslateAll() {
      for (ITranslatable entry : ENTRIES) {
         try {
            entry.retranslateI18n();
         } catch (Throwable var3) {
         }
      }
   }
}
