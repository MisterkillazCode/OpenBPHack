package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.BPHackChams;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({PlayerEntityRenderer.class})
public abstract class MixinBPHPlayerEntityRenderer {
   @Unique
   private BPHackChams chams;
   @Unique
   private static final Identifier BLANK = Identifier.of("minecraft", "textures/blank.png");

   @Unique
   private BPHackChams getChams() {
      if (this.chams == null) {
         this.chams = (BPHackChams)Modules.get().get(BPHackChams.class);
      }

      return this.chams;
   }

   @ModifyExpressionValue(
      method = {"renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Lnet/minecraft/client/model/ModelPart;Z)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/RenderLayers;entityTranslucent(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"
      )}
   )
   private RenderLayer onRenderArmTexture(
      RenderLayer original,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue commandQueue,
      int light,
      Identifier skinTexture,
      ModelPart modelPart,
      boolean sleeveVisible
   ) {
      BPHackChams c = this.getChams();
      if (c != null && c.isActive() && (Boolean)c.handEnabled.get()) {
         Identifier tex = c.handTexture.get() ? skinTexture : BLANK;
         return RenderLayers.entityTranslucent(tex);
      } else {
         return original;
      }
   }

   @WrapWithCondition(
      method = {"renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Lnet/minecraft/client/model/ModelPart;Z)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;)V"
      )}
   )
   private boolean onRenderArmColor(
      OrderedRenderCommandQueue instance, ModelPart modelPart, MatrixStack matrixStack, RenderLayer renderLayer, int light, int overlay, Sprite sprite
   ) {
      BPHackChams c = this.getChams();
      if (c != null && c.isActive() && (Boolean)c.handEnabled.get()) {
         instance.submitModelPart(modelPart, matrixStack, renderLayer, light, overlay, null, ((SettingColor)c.handColor.get()).getPacked(), null);
         return false;
      } else {
         return true;
      }
   }
}
