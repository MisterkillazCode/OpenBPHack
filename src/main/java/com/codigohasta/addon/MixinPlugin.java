package com.codigohasta.addon;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class MixinPlugin implements IMixinConfigPlugin {
   private static final String MIXIN_PACKAGE = "com.codigohasta.addon.mixin";
   private static boolean isSodiumPresent;

   public void onLoad(String mixinPackage) {
      try {
         isSodiumPresent = FabricLoader.getInstance().isModLoaded("sodium");
      } catch (Throwable var3) {
         isSodiumPresent = false;
      }
   }

   public String getRefMapperConfig() {
      return null;
   }

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      return mixinClassName.startsWith("com.codigohasta.addon.mixin.sodium") ? isSodiumPresent : true;
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public List<String> getMixins() {
      return null;
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}
