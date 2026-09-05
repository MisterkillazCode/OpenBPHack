package com.codigohasta.addon.mixin;

import com.codigohasta.addon.utils.KillAuraMoveFixState;
import com.codigohasta.addon.utils.leaveshack.Rotation;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura.RotationMode;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {KillAura.class},
   remap = false
)
public abstract class MixinKillAura {
   @Shadow
   private Setting<RotationMode> rotation;
   @Unique
   private Setting<KillAuraMoveFixState.Mode> img$movefixMode;
   @Unique
   private boolean img$tickCancelled;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void onInit(CallbackInfo ci) {
      Settings s = ((Module)this).settings;
      this.img$movefixMode = s.getDefaultGroup()
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("move-fix-mode"))
                     .description(
                        "IMG-side fix for the Grim 'bounce / setback' when KillAura silently rotates while attacking. KeepSprint = leave sprint untouched so velocity stays continuous (no bounce). StopSprint = force sprint off while attacking (legacy, can bounce). None = disable."
                     ))
                  .defaultValue(KillAuraMoveFixState.Mode.KeepSprint))
               .build()
         );
   }

   @Inject(
      method = {"onTick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onTickHead(Pre event, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) {
         ci.cancel();
         KillAuraMoveFixState.active = false;
         KillAuraMoveFixState.mode = KillAuraMoveFixState.Mode.None;
         KillAuraMoveFixState.attacking = false;
         Rotation.rotation = false;
      } else {
         KillAuraMoveFixState.mode = (KillAuraMoveFixState.Mode)this.img$movefixMode.get();
         KillAuraMoveFixState.active = KillAuraMoveFixState.mode != KillAuraMoveFixState.Mode.None;
         this.img$tickCancelled = false;
      }
   }

   @Inject(
      method = {"onTick"},
      at = {@At("RETURN")}
   )
   private void onTickReturn(Pre event, CallbackInfo ci) {
      if (!this.img$tickCancelled) {
         KillAura self = (KillAura)this;
         KillAuraMoveFixState.attacking = self.attacking;
         Entity target = self.getTarget();
         if (KillAuraMoveFixState.active && this.shouldRotateThisTick(self, target)) {
            Rotation.snapAt((float)Rotations.getYaw(target), (float)Rotations.getPitch(target, Target.Body));
         } else {
            Rotation.rotation = false;
         }
      }
   }

   @Unique
   private boolean shouldRotateThisTick(KillAura self, Entity target) {
      if (target == null) {
         return false;
      } else {
         RotationMode mode = (RotationMode)this.rotation.get();
         if (mode == RotationMode.None) {
            return false;
         } else {
            return mode == RotationMode.Always ? true : self.attacking;
         }
      }
   }

   @Inject(
      method = {"onDeactivate"},
      at = {@At("HEAD")}
   )
   private void onDeactivateHead(CallbackInfo ci) {
      KillAuraMoveFixState.active = false;
      KillAuraMoveFixState.mode = KillAuraMoveFixState.Mode.None;
      KillAuraMoveFixState.attacking = false;
      Rotation.rotation = false;
   }
}
