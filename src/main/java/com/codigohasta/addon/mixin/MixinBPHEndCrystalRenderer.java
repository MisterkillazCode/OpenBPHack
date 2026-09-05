package com.codigohasta.addon.mixin;

import com.codigohasta.addon.modules.BPHackChams;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EndCrystalEntityRenderer.class})
public abstract class MixinBPHEndCrystalRenderer {
   @Shadow
   @Final
   @Mutable
   private static RenderLayer END_CRYSTAL;
   @Shadow
   @Final
   private static Identifier TEXTURE;
   @Unique
   private static final Identifier BLANK = Identifier.of("minecraft", "textures/blank.png");
   @Unique
   private static RenderLayer END_CRYSTAL_BLANK;
   @Unique
   private BPHackChams chams;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void onInit(CallbackInfo ci) {
      this.chams = (BPHackChams)Modules.get().get(BPHackChams.class);
      END_CRYSTAL_BLANK = RenderLayers.entityTranslucent(BLANK);
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"},
      at = {@At("HEAD")}
   )
   private void onRenderHead(
      EndCrystalEntityRenderState state, MatrixStack matrixStack, OrderedRenderCommandQueue commandQueue, CameraRenderState cameraState, CallbackInfo ci
   ) {
      if (this.chams != null && this.chams.customCrystal()) {
         END_CRYSTAL = this.chams.textureEnabled.get() ? RenderLayers.entityTranslucent(TEXTURE) : END_CRYSTAL_BLANK;
      }
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"
      )}
   )
   private void onScale(
      EndCrystalEntityRenderState state, MatrixStack matrixStack, OrderedRenderCommandQueue commandQueue, CameraRenderState cameraState, CallbackInfo ci
   ) {
      if (this.chams != null && this.chams.customCrystal() && (Double)this.chams.scale.get() != 1.0) {
         float s = ((Double)this.chams.scale.get()).floatValue();
         matrixStack.scale(s, s, s);
      }
   }

   @WrapWithCondition(
      method = {"render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
      )}
   )
   private <S> boolean onColor(
      OrderedRenderCommandQueue instance,
      Model<? super S> model,
      S state,
      MatrixStack matrixStack,
      RenderLayer renderLayer,
      int light,
      int overlay,
      int outlineColor,
      CrumblingOverlayCommand crumblingOverlay
   ) {
      if (this.chams != null && this.chams.customCrystal()) {
         instance.submitModel(
            model, state, matrixStack, END_CRYSTAL, light, overlay, ((SettingColor)this.chams.crystalColor.get()).getPacked(), null, outlineColor, null
         );
         return false;
      } else {
         return true;
      }
   }
}
