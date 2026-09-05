package com.codigohasta.addon.mixin;

import net.minecraft.client.particle.BillboardParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({BillboardParticle.class})
public interface BillboardParticleInvoker {
   @Invoker("setColor")
   void invokeSetColor(float var1, float var2, float var3);

   @Invoker("setAlpha")
   void invokeSetAlpha(float var1);
}
