package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.Ambience;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FogRenderer.class})
public class MixinAmbienceFogRenderer {
   @Inject(
      method = {"getFogColor(Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/world/ClientWorld;IF)Lorg/joml/Vector4f;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void onGetFogColor(Camera camera, float tickProgress, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
      if (Modules.get() != null) {
         Ambience ambience = (Ambience)Modules.get().get(Ambience.class);
         if (ambience != null && ambience.isActive()) {
            if ((Boolean)ambience.fogEnabled.get()) {
               cir.setReturnValue(((SettingColor)ambience.fogColor.get()).getVec4f());
            } else if ((Boolean)ambience.dimensionColorEnabled.get()) {
               cir.setReturnValue(((SettingColor)ambience.dimensionColor.get()).getVec4f());
            }
         }
      }
   }

   @ModifyVariable(
      method = {"applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"},
      at = @At("HEAD"),
      argsOnly = true,
      index = 4
   )
   private float modifyFogStart(float start) {
      if (Modules.get() == null) {
         return start;
      } else {
         Ambience ambience = (Ambience)Modules.get().get(Ambience.class);
         return ambience != null && ambience.isActive() && ambience.fogDistance.get() ? ((Double)ambience.fogStart.get()).floatValue() : start;
      }
   }

   @ModifyVariable(
      method = {"applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"},
      at = @At("HEAD"),
      argsOnly = true,
      index = 5
   )
   private float modifyFogEnd(float end) {
      if (Modules.get() == null) {
         return end;
      } else {
         Ambience ambience = (Ambience)Modules.get().get(Ambience.class);
         return ambience != null && ambience.isActive() && ambience.fogDistance.get() ? ((Double)ambience.fogEnd.get()).floatValue() : end;
      }
   }
}
