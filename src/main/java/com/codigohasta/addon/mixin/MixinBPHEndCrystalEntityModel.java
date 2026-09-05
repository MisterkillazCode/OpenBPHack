package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.BPHackChams;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({EndCrystalEntityModel.class})
public abstract class MixinBPHEndCrystalEntityModel {
   @Unique
   private BPHackChams chams;

   @Unique
   private BPHackChams getChams() {
      if (this.chams == null) {
         this.chams = (BPHackChams)Modules.get().get(BPHackChams.class);
      }

      return this.chams;
   }

   @ModifyExpressionValue(
      method = {"setAngles(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/EndCrystalEntityRenderer;getYOffset(F)F"
      )}
   )
   private float onBounce(float original, EndCrystalEntityRenderState state) {
      BPHackChams c = this.getChams();
      if (c != null && c.isActive() && (Boolean)c.crystalEnabled.get()) {
         float g = MathHelper.sin(state.age * 0.2F * ((Double)c.bounceSpeed.get()).floatValue()) / 2.0F + 0.5F;
         g = (g * g + g) * 0.4F * ((Double)c.bounceHeight.get()).floatValue();
         return g - 1.4F + ((Double)c.yOffset.get()).floatValue();
      } else {
         return original;
      }
   }

   @ModifyExpressionValue(
      method = {"setAngles(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;)V"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;age:F",
         ordinal = 0
      )}
   )
   private float onRotationSpeed(float original) {
      BPHackChams c = this.getChams();
      return c != null && c.isActive() && c.crystalEnabled.get() ? original * ((Double)c.spinSpeed.get()).floatValue() : original;
   }
}
