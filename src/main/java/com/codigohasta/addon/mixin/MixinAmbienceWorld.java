package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.Ambience;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({World.class})
public class MixinAmbienceWorld {
   @Inject(
      method = {"getTimeOfDay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetTimeOfDay(CallbackInfoReturnable<Long> info) {
      if (Modules.get() != null) {
         Ambience ambience = (Ambience)Modules.get().get(Ambience.class);
         if (ambience != null && ambience.isActive() && (Boolean)ambience.customTime.get()) {
            info.setReturnValue(((Integer)ambience.time.get()).longValue());
         }
      }
   }
}
