package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.Ambience;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.dimension.DimensionType.Skybox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SkyRendering.class})
public class MixinAmbienceSkyRendering {
   @Inject(
      method = {"updateRenderState"},
      at = {@At("TAIL")}
   )
   private void onUpdateRenderState(ClientWorld world, float tickProgress, Camera camera, SkyRenderState state, CallbackInfo ci) {
      if (Modules.get() != null) {
         Ambience ambience = (Ambience)Modules.get().get(Ambience.class);
         if (ambience != null && ambience.isActive()) {
            if ((Boolean)ambience.skyEnabled.get()) {
               state.skyColor = ((SettingColor)ambience.skyColor.get()).getPacked();
            }

            if ((Boolean)ambience.forceOverworld.get()) {
               state.skybox = Skybox.OVERWORLD;
            }
         }
      }
   }
}
