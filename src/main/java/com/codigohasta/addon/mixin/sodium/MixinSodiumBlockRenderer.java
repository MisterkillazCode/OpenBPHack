package com.codigohasta.addon.mixin.sodium;

import com.codigohasta.addon.modules.NoHurtCam;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   targets = {"net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer"}
)
public class MixinSodiumBlockRenderer {
   @Inject(
      method = {"renderModel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderModel(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
      NoHurtCam module = NoHurtCam.INSTANCE;
      if (module != null && module.isActive()) {
         if (module.hiddenBlock != null && pos.equals(module.hiddenBlock)) {
            ci.cancel();
         }
      }
   }
}
