package com.codigohasta.addon.mixin;

import com.codigohasta.addon.enums.ImgModes;
import com.codigohasta.addon.utils.epsilon.EpsilonMovementUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {NoFall.class},
   remap = false
)
public abstract class MixinNoFall {
   @Shadow
   private Setting<Boolean> pauseOnMace;
   @Unique
   private Setting<ImgModes.ImgMode> imgMode;
   @Unique
   private Setting<ImgModes.IntValues> imgLazyTicks;
   @Unique
   private int imgTick = 0;
   @Unique
   private int imgLastTick = -10;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void onInit(CallbackInfo ci) {
      SettingGroup sg = ((Module)this).settings.getDefaultGroup();
      this.imgMode = sg.add(
         ((Builder)((Builder)((Builder)new Builder().name("img-mode"))
                  .description("IMG custom Grim fall-damage bypass. Separate dropdown because the built-in mode enum cannot be extended."))
               .defaultValue(ImgModes.ImgMode.Off))
            .build()
      );
      this.imgLazyTicks = sg.add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("img-lazy-ticks")).description("Ticks between LazyBypassGrim emissions."))
                  .defaultValue(ImgModes.IntValues.Five))
               .visible(() -> this.imgMode.get() == ImgModes.ImgMode.LazyBypassGrim))
            .build()
      );
   }

   @Inject(
      method = {"onSendPacket"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSendPacketHead(Send event, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Module self = (Module)this;
      if (self.isActive() && mc.player != null) {
         if (this.imgMode.get() != ImgModes.ImgMode.Off) {
            ci.cancel();
            if (!mc.player.getAbilities().creativeMode) {
               if (!(Boolean)this.pauseOnMace.get() || !(mc.player.getMainHandStack().getItem() instanceof MaceItem)) {
                  if (event.packet instanceof PlayerMoveC2SPacket) {
                     if (mc.player.isOnGround()) {
                        this.imgTick++;
                        switch ((ImgModes.ImgMode)this.imgMode.get()) {
                           case BypassGrim:
                              mc.interactionManager.sendSequencedPacket(mc.world, seq -> new OnGroundOnly(true, mc.player.horizontalCollision));
                              mc.player.setOnGround(true);
                              break;
                           case LazyBypassGrim:
                              int period = ((ImgModes.IntValues)this.imgLazyTicks.get()).asInt();
                              if (this.imgTick - this.imgLastTick >= period) {
                                 this.imgLastTick = this.imgTick;
                                 mc.interactionManager.sendSequencedPacket(mc.world, seq -> new OnGroundOnly(true, mc.player.horizontalCollision));
                                 mc.player.setOnGround(true);
                              }
                              break;
                           case LazyGrimPlus:
                              mc.getNetworkHandler().sendPacket(EpsilonMovementUtil.createGrimPositionPacket(0.1));
                              break;
                           case DupFullFakeGround:
                              mc.getNetworkHandler().sendPacket(new OnGroundOnly(true, mc.player.horizontalCollision));
                              mc.getNetworkHandler().sendPacket(new OnGroundOnly(true, mc.player.horizontalCollision));
                              mc.player.setOnGround(true);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
