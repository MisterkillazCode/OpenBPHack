package com.codigohasta.addon.mixin;

import java.util.Collections;
import java.util.List;
import meteordevelopment.meteorclient.systems.modules.render.BreakIndicators;
import meteordevelopment.meteorclient.systems.modules.world.PacketMine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   value = {BreakIndicators.class},
   remap = false
)
public class MixinBreakIndicators {
   @Redirect(
      method = {"onRender"},
      at = @At(
         value = "FIELD",
         target = "Lmeteordevelopment/meteorclient/systems/modules/world/PacketMine;blocks:Ljava/util/List;"
      )
   )
   private List<?> redirectGetBlocks(PacketMine instance) {
      return instance == null ? Collections.emptyList() : instance.blocks;
   }
}
