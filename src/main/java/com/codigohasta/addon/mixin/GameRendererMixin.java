package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.Ambience;
import com.codigohasta.addon.modules.CustomFov;
import com.codigohasta.addon.modules.NoHurtCam;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Zoom;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GameRenderer.class})
public class GameRendererMixin {
   @Inject(
      method = {"getFov"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> info) {
      CustomFov module = (CustomFov)Modules.get().get(CustomFov.class);
      if (module != null && module.isActive()) {
         if (!changingFov) {
            info.setReturnValue(((Double)module.itemFov.get()).floatValue());
         } else {
            float fov = ((Double)module.fov.get()).floatValue();
            Zoom zoom = (Zoom)Modules.get().get(Zoom.class);
            if (zoom != null) {
               double scaling = zoom.getScaling();
               if (scaling > 1.0) {
                  fov /= (float)scaling;
               }
            }

            info.setReturnValue(fov);
         }
      }
   }

   @Inject(
      method = {"getNightVisionStrength"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void guardNightVisionStrength(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
      if (entity.getStatusEffect(StatusEffects.NIGHT_VISION) == null) {
         Ambience ambience = Modules.get() == null ? null : (Ambience)Modules.get().get(Ambience.class);
         if (ambience != null && ambience.isActive() && (Boolean)ambience.fullBright.get()) {
            cir.setReturnValue(1.0F);
         } else {
            cir.setReturnValue(0.0F);
         }
      }
   }

   @Redirect(
      method = {"tiltViewWhenHurt"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/entity/LivingEntity;hurtTime:I",
         opcode = 180
      )
   )
   private int noHurtCamHurtTime(LivingEntity entity) {
      NoHurtCam module = Modules.get() == null ? null : (NoHurtCam)Modules.get().get(NoHurtCam.class);
      return module != null && module.isActive() && module.shouldRemoveTilt() ? 0 : entity.hurtTime;
   }
}
