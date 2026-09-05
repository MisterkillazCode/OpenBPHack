package com.codigohasta.addon.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({MinecraftClient.class})
public interface MinecraftClientAccessor {
   @Accessor("itemUseCooldown")
   void setItemUseCooldown(int var1);

   @Accessor("itemUseCooldown")
   int getItemUseCooldown();
}
