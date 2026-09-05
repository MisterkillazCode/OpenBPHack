package com.codigohasta.addon.mixin;

import com.codigohasta.addon.enums.ImgModes;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {NoSlow.class},
   remap = false
)
public abstract class MixinNoSlow {
   @Unique
   private Setting<ImgModes.ImgSneakBypass> imgSneakBypass;
   @Unique
   private Setting<ImgModes.ImgPacketSneak> imgPacketSneak;
   @Unique
   private Setting<Boolean> imgBlockSlow;
   @Unique
   private Setting<Double> imgSneakComp;
   @Unique
   private int imgLastSwapTick = -10;
   @Unique
   private int imgTick = 0;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void onInit(CallbackInfo ci) {
      SettingGroup sg = ((Module)this).settings.getDefaultGroup();
      this.imgSneakBypass = sg.add(
         ((Builder)((Builder)((Builder)new Builder().name("img-sneak-bypass"))
                  .description("Grim sneak-slowness bypass. GrimLazy/V3 refresh sprint packets while slowed."))
               .defaultValue(ImgModes.ImgSneakBypass.Off))
            .build()
      );
      this.imgPacketSneak = sg.add(
         ((Builder)((Builder)((Builder)new Builder().name("img-packet-sneak")).description("Fake-sneak strategy. Only GrimFallFlying is wired up in 1.21.11."))
               .defaultValue(ImgModes.ImgPacketSneak.Off))
            .build()
      );
      this.imgBlockSlow = sg.add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("img-block-slow"))
                  .description("Bypass cobweb / powder-snow slowness with a velocity fix (when web mode is on)."))
               .defaultValue(false))
            .build()
      );
      this.imgSneakComp = sg.add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("img-sneak-comp"))
               .description("Velocity multiplier to counter sneak slowdown (1.0 = off)."))
            .defaultValue(1.0)
            .min(1.0)
            .sliderMax(2.0)
            .build()
      );
   }

   @Inject(
      method = {"onPreTick"},
      at = {@At("TAIL")}
   )
   private void onPreTickTail(Pre event, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.world != null) {
         this.imgTick++;
         if ((Double)this.imgSneakComp.get() > 1.0 && mc.player.isSneaking()) {
            Vec3d v = mc.player.getVelocity();
            double m = (Double)this.imgSneakComp.get();
            mc.player.setVelocity(v.x * m, v.y, v.z * m);
         }

         if ((Boolean)this.imgBlockSlow.get() && mc.player.isAlive()) {
            BlockState state = mc.world.getBlockState(mc.player.getBlockPos());
            boolean inWeb = state.isOf(Blocks.COBWEB) || state.isOf(Blocks.POWDER_SNOW);
            if (inWeb && this.imgSneakBypass.get() == ImgModes.ImgSneakBypass.GrimLazy) {
               boolean moving = mc.player.input.hasForwardMovement() || mc.player.input.getMovementInput().x != 0.0F;
               if (moving) {
                  Vec3d v = mc.player.getVelocity();
                  double speed = Math.hypot(v.x, v.z);
                  if (speed > 0.01) {
                     double mul = 0.64;
                     mc.player.setVelocity(v.x / speed * speed * mul, v.y, v.z / speed * speed * mul);
                  }
               }
            }
         }

         if (mc.player.isUsingItem() && this.imgSneakBypass.get() != ImgModes.ImgSneakBypass.Off) {
            if (this.imgTick - this.imgLastSwapTick >= 1) {
               this.imgLastSwapTick = this.imgTick;
               mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_SPRINTING));
            }

            mc.player.setSprinting(true);
         }
      }
   }
}
