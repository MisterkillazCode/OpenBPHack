package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.NoHurtCam;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BlockRenderManager.class})
public class MixinBlockRenderManager {
   @Inject(
      method = {"renderBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderBlock(
      BlockState state, BlockPos pos, BlockRenderView world, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, List<?> layers, CallbackInfo ci
   ) {
      NoHurtCam module = NoHurtCam.INSTANCE;
      if (module != null && module.isActive()) {
         if (module.hiddenBlock != null && pos.equals(module.hiddenBlock)) {
            ci.cancel();
         }
      }
   }
}
